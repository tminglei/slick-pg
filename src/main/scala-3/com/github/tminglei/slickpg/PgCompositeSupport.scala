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

sealed abstract class BaseLevel[D <: BaseLevel[?, ?] , I <: BaseLevel[?, ?]] {
  def i: Int
  final type Dec = D
  final type Inc = I
  def -- : Dec
  def ++ : Inc
}
sealed abstract class Level[D <: BaseLevel[?, ?] , I <: BaseLevel[?, ?]](val -- : D, val ++ : I, val i: Int) extends BaseLevel[D, I]
case object Level_1 extends BaseLevel[Level_1.type, Level0.type]{
  def i: Int = -1
  override def -- = this
  override def ++ = Level0
}
case object Level0 extends Level[Level_1.type, Level1.type](Level_1, Level1, 0)
case object Level1 extends Level[Level0.type, Level2.type](Level0, Level2, 1)
case object Level2 extends Level[Level1.type, Level3.type](Level1, Level3, 2)
case object Level3 extends Level[Level2.type, Level4.type](Level2, Level4, 3)
case object Level4 extends Level[Level3.type, Level5.type](Level3, Level5, 4)
case object Level5 extends Level[Level4.type, Level6.type](Level4, Level6, 5)
case object Level6 extends BaseLevel[Level5.type, Level6.type]{
  def i: Int = 6
  override def -- = Level5
  override def ++ = this
}


case class Args(args: Any*)

sealed trait TokenConverter[T, L <: BaseLevel[?, ?]](val l: L) {
  type Type
  type Level = L
  type LevelInc = l.Inc
  def levelInc: LevelInc = l.++
  type LevelDec = l.Dec
  def levelDec: LevelDec = l.--
  def fromToken(token: Token): T

  def toToken(value: T): Token
}


trait PgCompositeSupport extends utils.PgCommonJdbcTypes with array.PgArrayJdbcTypes { driver: PostgresProfile =>

  protected lazy val emptyMembersAsNull = true

  inline implicit def baseConverter[T, L <: BaseLevel[?, ?]: ValueOf](using fromString: RegisteredTypeConverter[String, T], toStringFn: RegisteredTypeConverter[T, String]): TokenConverter[T, L] = new TokenConverter[T, L](valueOf[L]) {
    def fromToken(token: Token): T = if (token == Null) null.asInstanceOf[T] else fromString.convert(getString(token, valueOf[L].i))

    def toToken(value: T): Token = if (value == null) Null else Chunk(toStringFn.convert(value))
  }

  //---
  inline def createCompositeJdbcType[T <: Struct : Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): GenericJdbcType[T] = {
    lazy val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    val c = util.derived[T, Level_1.type ]
    new GenericJdbcType[T](sqlTypeName, { input =>
      val root = grouping(Tokenizer.tokenize(input))
      c.fromToken(root)
    }, value => createString(c.toToken(value)))
  }

  inline def createCompositeArrayJdbcType[T <: Struct: Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): AdvancedArrayJdbcType[T] = {
    lazy val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    implicit val bar: TokenConverter[T, Level0.type ] = util.derived[T, Level0.type ]
    val c: TokenConverter[Seq[T], Level_1.type] = util.seqConverter[T, Level0.type](bar)
    new AdvancedArrayJdbcType[T](sqlTypeName, { input =>
      val root = grouping(Tokenizer.tokenize(input))
      c.fromToken(root)
    }, value => createString(c.toToken(value)))
  }

  /// Plain SQL support
  inline def nextComposite[T <: Struct: Mirror.Of: ClassTag](r: PositionedResult, cl: ClassLoader = getClass.getClassLoader): Option[T] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    val c = util.derived[T, Level_1.type]
    r.nextStringOption().map { input =>
      val root = grouping(Tokenizer.tokenize(input))
      c.fromToken(root)
    }
  }

  inline def nextCompositeArray[T <: Struct: Mirror.Of: ClassTag](r: PositionedResult, cl: ClassLoader = getClass.getClassLoader): Option[Seq[T]] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    implicit val bar: TokenConverter[T, Level0.type] = util.derived[T, Level0.type]
    val c: TokenConverter[Seq[T], Level_1.type] = util.seqConverter[T, Level0.type](bar)
    r.nextStringOption().map { input =>
      val root = grouping(Tokenizer.tokenize(input))
      c.fromToken(root)
    }
  }

  inline def createCompositeSetParameter[T <: Struct: Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[T] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    val c = util.derived[T, Level_1.type]
    utils.PlainSQLUtils.mkSetParameter[T](sqlTypeName, value => createString(c.toToken(value)))
  }

  inline def createCompositeOptionSetParameter[T <: Struct: Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[Option[T]] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    val c = util.derived[T, Level_1.type]
    utils.PlainSQLUtils.mkOptionSetParameter[T](sqlTypeName, value => createString(c.toToken(value)))
  }

  inline def createCompositeArraySetParameter[T <: Struct: Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[Seq[T]]  = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    implicit val bar: TokenConverter[T, Level0.type] = util.derived[T, Level0.type]
    val c: TokenConverter[Seq[T], Level_1.type] = util.seqConverter[T, Level0.type](bar)
    utils.PlainSQLUtils.mkArraySetParameter[T](sqlTypeName, seqToStr = Some(value => createString(c.toToken(value))))
  }

  inline def createCompositeOptionArraySetParameter[T <: Struct: Mirror.Of: ClassTag](sqlTypeName: String, cl: ClassLoader = getClass.getClassLoader): SetParameter[Option[Seq[T]]] = {
    val util = new PgCompositeSupportUtils(cl, emptyMembersAsNull)
    implicit val bar: TokenConverter[T, Level0.type] = util.derived[T, Level0.type]
    val c: TokenConverter[Seq[T], Level_1.type] = util.seqConverter[T, Level0.type](bar)
    utils.PlainSQLUtils.mkArrayOptionSetParameter[T](sqlTypeName, seqToStr = Some(value => createString(c.toToken(value))))
  }
}

class PgCompositeSupportUtils(cl: ClassLoader, emptyMembersAsNull: Boolean) {
  import scala.deriving.*
  import scala.compiletime.{error, erasedValue, summonInline}

  def seqConverter[T, L <: BaseLevel[?, ?]](delegate: TokenConverter[T, L]): TokenConverter[Seq[T], delegate.LevelDec] = new TokenConverter[Seq[T], delegate.LevelDec](delegate.levelDec) {
    def fromToken(token: Token): Seq[T] =
      if (token == Null) null else getChildren(token).map(delegate.fromToken)

    def toToken(value: Seq[T]): Token = value match {
      case vList: Seq[Any] => {
        val members = Open("{") +: vList.map(delegate.toToken) :+ Close("}")
        GroupToken(members)
      }
      case null => Null
      case _ => throw new IllegalArgumentException("WRONG type value: " + value)
    }
  }
  def optConverter[T, L <: BaseLevel[?, ?]](delegate: TokenConverter[T, L]): TokenConverter[Option[T], L] = new TokenConverter[Option[T], L](delegate.l) {
    private def isNull(token: Token): Boolean = token match {
      case g@GroupToken(_) if emptyMembersAsNull => getChildren(g).forall(isNull)
      case Null => true
      case _ => false
    }

    def fromToken(token: Token): Option[T] =
      if (isNull(token)) None else Some(delegate.fromToken(token))

    def toToken(value: Option[T]): Token = value match {
      case Some(v: T) => delegate.toToken(v)
      case None => Null
      case _ => throw new IllegalArgumentException("WRONG type value: " + value)
    }
  }

  inline def summonInstances[Elems <: Tuple, L <: BaseLevel[?, ?]: ValueOf]: List[TokenConverter[?, ?]] = {
    val v = valueOf[L]
    type Inc = v.Inc
    implicit val vo: ValueOf[Inc] = ValueOf[Inc](v.++)
    inline erasedValue[Elems] match {
      case _: (elem *: elems) => deriveOrSummon[elem, Inc] :: summonInstances[elems, L]
      case _: EmptyTuple => Nil
    }
  }

  inline def deriveOrSummon[ Elem, L <: BaseLevel[?, ?]: ValueOf]: TokenConverter[Elem, L] = summonFrom {
      case m: Mirror.Of[Elem & Struct] =>
        summonFrom { case ct: ClassTag[Elem & Struct] => derived[Elem & Struct, L].asInstanceOf[TokenConverter[Elem, L]] }
      case _: (Elem <:< Option[Any]) =>
        inline erasedValue[Elem] match {
          case _: Option[t] =>
            val elemConverter: TokenConverter[t, L] = deriveOrSummon[t, L]
            optConverter(elemConverter).asInstanceOf[TokenConverter[Elem, L]]
        }
      case _: (Elem <:< Seq[Any]) =>
        inline erasedValue[Elem] match {
          case _: Seq[t] =>
            val v = valueOf[L]
            type Inc = v.Inc
            implicit val vo: ValueOf[Inc] = ValueOf[Inc](v ++)
            val elemConverter: TokenConverter[t, Inc] = deriveOrSummon[t, Inc]
            seqConverter(elemConverter).asInstanceOf[TokenConverter[Elem, L]]
        }
      case _ => summonInline[TokenConverter[Elem, L]]
  }

  def convertProduct[T <: Struct: ClassTag, L <: BaseLevel[?, ?]: ValueOf](p: Mirror.ProductOf[T], elems: => List[TokenConverter[?, ?]]): TokenConverter[T, L] =
    new TokenConverter[T, L](valueOf[L]) {
      def fromToken(token: Token): T =
        if (token == Null) null.asInstanceOf[T]
        else {
          val args =
            getChildren(token)
              .zip(elems)
              .map { case (token, converter) => converter.fromToken(token) }

          classTag.runtimeClass.getConstructors.head.newInstance(args:_*).asInstanceOf[T]
        }

      def toToken(value: T): Token =
        if (value == null) Null
        else {
          val v = valueOf[L]
          type Inc = v.Inc
          implicit val vo: ValueOf[Inc] = ValueOf(v.++)
          val tokens = value.asInstanceOf[Product].productIterator.zip(elems).toSeq.map({
            case (v, converter) => converter.asInstanceOf[TokenConverter[Any, Inc]].toToken(v)
          })
          val members = Open("(") +: tokens :+ Close(")")
          GroupToken(members)
        }
    }

  inline def derived[T <: Struct: ClassTag, L <: BaseLevel[?, ?]: ValueOf](using m: Mirror.Of[T]): TokenConverter[T, L] = {
    lazy val elemInstances = summonInstances[m.MirroredElemTypes, L]
    inline m match
      case p: Mirror.ProductOf[T] => convertProduct(p, elemInstances)
  }

}
