package com.github.tminglei.slickpg

import scala.reflect.{ClassTag, classTag}
import composite.Struct
import slick.jdbc.{PositionedResult, PostgresProfile}
import slick.jdbc.SetParameter

import scala.compiletime.{error, erasedValue, summonInline, summonFrom}
import utils.PgTokenHelper._
import utils.TypeConverters._
import utils.RegisteredTypeConverter

case class TokenConverter[T](level: Int, fromTokenAndLevel: Int => Token => T, toToken: T => Token) {
  def fromToken(t: Token): T = fromTokenAndLevel(level)(t)
  def withLevel(l: Int): TokenConverter[T] = copy(level = l)
}


trait PgCompositeSupport extends utils.PgCommonJdbcTypes with array.PgArrayJdbcTypes { driver: PostgresProfile =>

  protected lazy val emptyMembersAsNull = true

  inline implicit def baseConverter[T](using fromString: RegisteredTypeConverter[String, T], toStringFn: RegisteredTypeConverter[T, String]): TokenConverter[T] = new TokenConverter[T](0,
    level => token => if (token == Null) null.asInstanceOf[T] else fromString.convert(getString(token, level)),
    value => if (value == null) Null else Chunk(toStringFn.convert(value)))

  //---
  inline def createCompositeJdbcType[T <: Struct: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): GenericJdbcType[T] = {
    lazy val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    new GenericJdbcType[T](sqlTypeName, util.mkCompositeFromString[T], util.mkStringFromComposite[T])
  }

  inline def createCompositeArrayJdbcType[T <: Struct: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): AdvancedArrayJdbcType[T] = {
    lazy val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    new AdvancedArrayJdbcType[T](sqlTypeName, util.mkCompositeSeqFromString[T], util.mkStringFromCompositeSeq[T])
  }

  /// Plain SQL support
  inline def nextComposite[T <: Struct: ClassTag](r: PositionedResult, cl: ClassLoader = getClass.getClassLoader): Option[T] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    r.nextStringOption().map(util.mkCompositeFromString[T])
  }

  inline def nextCompositeArray[T <: Struct: ClassTag](r: PositionedResult, cl: ClassLoader = getClass.getClassLoader): Option[Seq[T]] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    r.nextStringOption().map(util.mkCompositeSeqFromString[T])
  }

  inline def createCompositeSetParameter[T <: Struct: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[T] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    utils.PlainSQLUtils.mkSetParameter[T](sqlTypeName, util.mkStringFromComposite[T])
  }

  inline def createCompositeOptionSetParameter[T <: Struct: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[Option[T]] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    utils.PlainSQLUtils.mkOptionSetParameter[T](sqlTypeName, util.mkStringFromComposite[T])
  }

  inline def createCompositeArraySetParameter[T <: Struct: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[Seq[T]]  = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    utils.PlainSQLUtils.mkArraySetParameter[T](sqlTypeName, seqToStr = Some(util.mkStringFromCompositeSeq[T]))
  }

  inline def createCompositeOptionArraySetParameter[T <: Struct: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[Option[Seq[T]]] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    utils.PlainSQLUtils.mkArrayOptionSetParameter[T](sqlTypeName, seqToStr = Some(util.mkStringFromCompositeSeq[T]))
  }
}

class PgCompositeSupportUtils(cl: ClassLoader, emptyMembersAsNull: Boolean) {
  import scala.deriving.*
  import scala.compiletime.{error, erasedValue, summonInline}

  inline def mkCompositeFromString[T] = {
    val c = deriveOrSummon[T](-1)
    (input: String) =>
      val root = grouping(Tokenizer.tokenize(input))
      c.fromToken(root)
    }

  inline def mkStringFromComposite[T] = {
    val c = deriveOrSummon[T](-1)
    (value: T) => createString(c.toToken(value))
  }

  inline def mkCompositeSeqFromString[T] = {
    val c: TokenConverter[Seq[T]] = deriveOrSummon[Seq[T]](-1)
    (input: String) =>
      val root = grouping(Tokenizer.tokenize(input))
      c.fromToken(root)
  }

  inline def mkStringFromCompositeSeq[T] = {
    val c: TokenConverter[Seq[T]] = deriveOrSummon[Seq[T]](-1)
    (value: Seq[T]) => createString(c.toToken(value))
  }

  def seqConverter[T](delegate: TokenConverter[T]): TokenConverter[Seq[T]] = TokenConverter[Seq[T]](delegate.level - 1,
    _ => token => if (token == Null) null else getChildren(token).map(delegate.fromToken),
    {
      case vList: Seq[Any] => {
        val members = Open("{") +: vList.map(delegate.toToken) :+ Close("}")
        GroupToken(members)
      }
      case null => Null
      case value => throw new IllegalArgumentException("WRONG type value: " + value)
    })

  private def isNull(token: Token): Boolean = token match {
    case g@GroupToken(_) if emptyMembersAsNull => getChildren(g).forall(isNull)
    case Null => true
    case _ => false
  }

  def optConverter[T](delegate: TokenConverter[T]): TokenConverter[Option[T]] = new TokenConverter[Option[T]](delegate.level,
    _ => token => if (isNull(token)) None else Some(delegate.fromToken(token)),
    {
      case Some(v: T) => delegate.toToken(v)
      case None => Null
      case value => throw new IllegalArgumentException("WRONG type value: " + value)
    })

  inline def summonInstances[Elems <: Tuple](i: Int): List[TokenConverter[?]] = {
    inline erasedValue[Elems] match {
      case _: (elem *: elems) => deriveOrSummon[elem](i + 1) :: summonInstances[elems](i)
      case _: EmptyTuple => Nil
    }
  }

  inline def deriveOrSummon[Elem](i: Int): TokenConverter[Elem] = summonFrom {
      case m: Mirror.Of[Elem & Struct] =>
        summonFrom { case ct: ClassTag[Elem & Struct] => derived[Elem & Struct](i).asInstanceOf[TokenConverter[Elem]] }
      case _: (Elem <:< Option[Any]) =>
        inline erasedValue[Elem] match {
          case _: Option[t] =>
            val elemConverter: TokenConverter[t] = deriveOrSummon[t](i)
            optConverter(elemConverter).asInstanceOf[TokenConverter[Elem]]
        }
      case _: (Elem <:< Seq[Any]) =>
        inline erasedValue[Elem] match {
          case _: Seq[t] =>
            val elemConverter: TokenConverter[t] = deriveOrSummon[t](i + 1)
            seqConverter(elemConverter).asInstanceOf[TokenConverter[Elem]]
        }
      case _ => summonInline[TokenConverter[Elem]].withLevel(i)
  }

  def convertProduct[T <: Struct: ClassTag](i: Int)(p: Mirror.ProductOf[T], elems: => List[TokenConverter[?]]): TokenConverter[T] =
    new TokenConverter[T](i, _ => token =>
        if (token == Null) null.asInstanceOf[T]
        else {
          val args =
            getChildren(token)
              .zip(elems)
              .map { case (token, converter) => converter.fromToken(token) }

          classTag.runtimeClass.getConstructors.head.newInstance(args:_*).asInstanceOf[T]
        }, value =>
        if (value == null) Null
        else {
          val tokens = value.asInstanceOf[Product].productIterator.zip(elems).toSeq.map({
            case (v, converter) => converter.asInstanceOf[TokenConverter[Any]].toToken(v)
          })
          val members = Open("(") +: tokens :+ Close(")")
          GroupToken(members)
        })

  inline def derived[T <: Struct: ClassTag](i: Int)(using m: Mirror.Of[T]): TokenConverter[T] = {
    lazy val elemInstances = summonInstances[m.MirroredElemTypes](i)
    inline m match
      case p: Mirror.ProductOf[T] => convertProduct[T](i)(p, elemInstances)
  }

}
