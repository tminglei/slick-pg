package com.github.tminglei.slickpg

import izumi.reflect.macrortti.LightTypeTag
import izumi.reflect.{Tag => TTag}

import scala.reflect.{ClassTag, classTag}
import composite.Struct
import slick.jdbc.{PositionedResult, PostgresProfile}
import slick.jdbc.SetParameter

import scala.annotation.unchecked.uncheckedVariance
import scala.deriving.*
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
  inline def createCompositeJdbcType[T <: Struct : Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): GenericJdbcType[T] = {
    lazy val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    val c = util.derived[T](-1)
    new GenericJdbcType[T](sqlTypeName, { input =>
      val root = grouping(Tokenizer.tokenize(input))
      c.fromToken(root)
    }, value => createString(c.toToken(value)))
  }

  inline def createCompositeArrayJdbcType[T <: Struct: Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): AdvancedArrayJdbcType[T] = {
    lazy val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    implicit val bar: TokenConverter[T] = util.derived[T](0)
    val c: TokenConverter[Seq[T]] = util.seqConverter[T](bar)
    new AdvancedArrayJdbcType[T](sqlTypeName, { input =>
      val root = grouping(Tokenizer.tokenize(input))
      c.fromToken(root)
    }, value => createString(c.toToken(value)))
  }

  /// Plain SQL support
  inline def nextComposite[T <: Struct: Mirror.Of: ClassTag](r: PositionedResult, cl: ClassLoader = getClass.getClassLoader): Option[T] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    val c = util.derived[T](-1)
    r.nextStringOption().map { input =>
      val root = grouping(Tokenizer.tokenize(input))
      c.fromToken(root)
    }
  }

  inline def nextCompositeArray[T <: Struct: Mirror.Of: ClassTag](r: PositionedResult, cl: ClassLoader = getClass.getClassLoader): Option[Seq[T]] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    implicit val bar: TokenConverter[T] = util.derived[T](0)
    val c: TokenConverter[Seq[T]] = util.seqConverter[T](bar)
    r.nextStringOption().map { input =>
      val root = grouping(Tokenizer.tokenize(input))
      c.fromToken(root)
    }
  }

  inline def createCompositeSetParameter[T <: Struct: Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[T] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    val c = util.derived[T](-1)
    utils.PlainSQLUtils.mkSetParameter[T](sqlTypeName, value => createString(c.toToken(value)))
  }

  inline def createCompositeOptionSetParameter[T <: Struct: Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[Option[T]] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    val c = util.derived[T](-1)
    utils.PlainSQLUtils.mkOptionSetParameter[T](sqlTypeName, value => createString(c.toToken(value)))
  }

  inline def createCompositeArraySetParameter[T <: Struct: Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[Seq[T]]  = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    implicit val bar: TokenConverter[T] = util.derived[T](0)
    val c: TokenConverter[Seq[T]] = util.seqConverter[T](bar)
    utils.PlainSQLUtils.mkArraySetParameter[T](sqlTypeName, seqToStr = Some(value => createString(c.toToken(value))))
  }

  inline def createCompositeOptionArraySetParameter[T <: Struct: Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[Option[Seq[T]]] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    implicit val bar: TokenConverter[T] = util.derived[T](0)
    val c: TokenConverter[Seq[T]] = util.seqConverter[T](bar)
    utils.PlainSQLUtils.mkArrayOptionSetParameter[T](sqlTypeName, seqToStr = Some(value => createString(c.toToken(value))))
  }
}

class PgCompositeSupportUtils(cl: ClassLoader, emptyMembersAsNull: Boolean) {
  import scala.deriving.*
  import scala.compiletime.{error, erasedValue, summonInline}

  def seqConverter[T](delegate: TokenConverter[T]): TokenConverter[Seq[T]] = TokenConverter[Seq[T]](delegate.level - 1,
    level => token => if (token == Null) null else getChildren(token).map(delegate.fromToken),
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
    level => token => if (isNull(token)) None else Some(delegate.fromToken(token)),
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
    new TokenConverter[T](i, level => token =>
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
