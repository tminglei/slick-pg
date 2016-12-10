package com.github.tminglei.slickpg

import scala.reflect.runtime.{universe => u}
import scala.reflect.ClassTag
import composite.Struct
import slick.jdbc.{PositionedResult, PostgresProfile}

trait PgCompositeSupport extends utils.PgCommonJdbcTypes with array.PgArrayJdbcTypes { driver: PostgresProfile =>

  def createCompositeJdbcType[T <: Struct](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader)(implicit ev: u.TypeTag[T], tag: ClassTag[T]) = {
    val util = new PgCompositeSupportUtils(cl)
    new GenericJdbcType[T](sqlTypeName, util.mkCompositeFromString[T], util.mkStringFromComposite[T])
  }

  def createCompositeArrayJdbcType[T <: Struct](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader)(implicit ev: u.TypeTag[T], tag: ClassTag[T]) = {
    val util = new PgCompositeSupportUtils(cl)
    new AdvancedArrayJdbcType[T](sqlTypeName, util.mkCompositeSeqFromString[T], util.mkStringFromCompositeSeq[T])
  }

  /// Plain SQL support
  def nextComposite[T <: Struct](r: PositionedResult, cl: ClassLoader = getClass.getClassLoader)(implicit ev: u.TypeTag[T], tag: ClassTag[T]) = {
    val util = new PgCompositeSupportUtils(cl)
    r.nextStringOption().map(util.mkCompositeFromString[T])
  }
  def nextCompositeArray[T <: Struct](r: PositionedResult, cl: ClassLoader = getClass.getClassLoader)(implicit ev: u.TypeTag[T], tag: ClassTag[T]) = {
    val util = new PgCompositeSupportUtils(cl)
    r.nextStringOption().map(util.mkCompositeSeqFromString[T])
  }

  def createCompositeSetParameter[T <: Struct](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader)(implicit ev: u.TypeTag[T], tag: ClassTag[T]) = {
    val util = new PgCompositeSupportUtils(cl)
    utils.PlainSQLUtils.mkSetParameter[T](sqlTypeName, util.mkStringFromComposite[T])
  }
  def createCompositeOptionSetParameter[T <: Struct](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader)(implicit ev: u.TypeTag[T], tag: ClassTag[T]) = {
    val util = new PgCompositeSupportUtils(cl)
    utils.PlainSQLUtils.mkOptionSetParameter[T](sqlTypeName, util.mkStringFromComposite[T])
  }
  def createCompositeArraySetParameter[T <: Struct](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader)(implicit ev: u.TypeTag[T], tag: ClassTag[T]) = {
    val util = new PgCompositeSupportUtils(cl)
    utils.PlainSQLUtils.mkArraySetParameter[T](sqlTypeName, seqToStr = Some(util.mkStringFromCompositeSeq[T]))
  }
  def createCompositeOptionArraySetParameter[T <: Struct](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader)(implicit ev: u.TypeTag[T], tag: ClassTag[T]) = {
    val util = new PgCompositeSupportUtils(cl)
    utils.PlainSQLUtils.mkArrayOptionSetParameter[T](sqlTypeName, seqToStr = Some(util.mkStringFromCompositeSeq[T]))
  }
}

class PgCompositeSupportUtils(cl: ClassLoader) {
  import utils.PgTokenHelper._
  import utils.TypeConverters._

  def mkCompositeFromString[T <: Struct](implicit ev: u.TypeTag[T]): (String => T) = {
    val converter = mkTokenConverter(u.typeOf[T])
    (input: String) => {
      val root = grouping(Tokenizer.tokenize(input))
      converter.fromToken(root).asInstanceOf[T]
    }
  }

  def mkStringFromComposite[T <: Struct](implicit ev: u.TypeTag[T]): (T => String) = {
    val converter = mkTokenConverter(u.typeOf[T])
    (value: T) => {
      createString(converter.toToken(value))
    }
  }

  def mkCompositeSeqFromString[T <: Struct](implicit ev: u.TypeTag[Seq[T]]): (String => Seq[T]) = {
    val converter = mkTokenConverter(u.typeOf[Seq[T]])
    (input: String) => {
      val root = grouping(Tokenizer.tokenize(input))
      converter.fromToken(root).asInstanceOf[Seq[T]]
    }
  }

  def mkStringFromCompositeSeq[T <: Struct](implicit ev: u.TypeTag[Seq[T]]): (Seq[T] => String) = {
    val converter = mkTokenConverter(u.typeOf[Seq[T]])
    (vList: Seq[T]) => {
      createString(converter.toToken(vList))
    }
  }

  ///
  def mkTokenConverter(theType: u.Type, level: Int = -1)(implicit ev: u.TypeTag[String]): TokenConverter = {
    theType match {
      case tpe if tpe <:< u.typeOf[Struct] => {
        val constructor = tpe.decl(u.termNames.CONSTRUCTOR).asMethod
        val convList = constructor.paramLists.head.map(_.typeSignature).map(mkTokenConverter(_, level +1))
        CompositeConverter(tpe, convList)
      }
      case tpe if tpe.typeConstructor =:= u.typeOf[Option[_]].typeConstructor => {
        val pType = tpe.asInstanceOf[u.TypeRef].args(0)
        OptionConverter(mkTokenConverter(pType, level))
      }
      case tpe if tpe.typeConstructor <:< u.typeOf[Seq[_]].typeConstructor => {
        val eType = tpe.asInstanceOf[u.TypeRef].args(0)
        SeqConverter(mkTokenConverter(eType, level +1))
      }
      case tpe => {
        val fromString = find(u.typeOf[String], tpe).map(_.conv.asInstanceOf[(String => Any)])
          .getOrElse(throw new IllegalArgumentException(s"Converter NOT FOUND for 'String' => '$tpe'"))
        val ToString = find(tpe, u.typeOf[String]).map(_.conv.asInstanceOf[(Any => String)])
          .getOrElse((v: Any) => v.toString)
        SimpleConverter(fromString, ToString, level)
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////
  sealed trait TokenConverter {
    def fromToken(token: Token): Any
    def toToken(value: Any): Token
  }

  case class SimpleConverter(fromString: (String => Any), ToString: (Any => String), level: Int) extends TokenConverter {
    def fromToken(token: Token): Any =
      if (token == Null) null else fromString(getString(token, level))
    def toToken(value: Any): Token =
      if (value == null) Null else Chunk(ToString(value))
  }

  case class CompositeConverter(theType: u.Type, convList: List[TokenConverter]) extends TokenConverter {
    private val constructor = theType.decl(u.termNames.CONSTRUCTOR).asMethod
    private val fieldList = constructor.paramLists.head.map(t => theType.decl(t.name).asTerm)

    def fromToken(token: Token): Any =
      if (token == Null) null
      else {
        val args = getChildren(token).zip(convList).map({
          case (token, converter) => converter.fromToken(token)
        })
        u.runtimeMirror(cl).reflectClass(theType.typeSymbol.asClass).reflectConstructor(constructor)
          .apply(args: _*)
      }
    def toToken(value: Any): Token =
      if (value == null) Null
      else {
        val instanceMirror = u.runtimeMirror(cl).reflect(value)
        val tokens = fieldList.zip(convList).map({
          case (field, converter) => converter.toToken(instanceMirror.reflectField(field).get)
        })
        val members = Open("(") +: tokens :+ Close(")")
        GroupToken(members)
      }
  }

  case class OptionConverter(delegate: TokenConverter) extends TokenConverter {
    def fromToken(token: Token): Any =
      if (token == Null) None else Some(delegate.fromToken(token))
    def toToken(value: Any): Token = value match {
      case Some(v) => delegate.toToken(v)
      case None => Null
      case _ => throw new IllegalArgumentException("WRONG type value: " + value)
    }
  }

  case class SeqConverter(delegate: TokenConverter) extends TokenConverter {
    def fromToken(token: Token): Any =
      if (token == Null) null else getChildren(token).map(delegate.fromToken)
    def toToken(value: Any): Token = value match {
      case vList: Seq[Any] => {
        val members = Open("{") +: vList.map(delegate.toToken) :+ Close("}")
        GroupToken(members)
      }
      case null => Null
      case _ => throw new IllegalArgumentException("WRONG type value: " + value)
    }
  }
}

object PgCompositeSupportUtils extends PgCompositeSupportUtils(getClass.getClassLoader)
