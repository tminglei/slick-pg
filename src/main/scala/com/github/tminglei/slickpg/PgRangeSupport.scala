package com.github.tminglei.slickpg

import java.sql.{Date, Timestamp}
import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}
import scala.reflect.classTag

// edge type definitions
sealed trait EdgeType
case object `[_,_)` extends EdgeType
case object `(_,_]` extends EdgeType
case object `(_,_)` extends EdgeType
case object `[_,_]` extends EdgeType
case object `empty` extends EdgeType

import PgRangeSupportUtils._

case class Range[T](start: Option[T], end: Option[T], edge: EdgeType) {

  def as[A](convert: (T => A)): Range[A] = {
    new Range[A](start.map(convert), end.map(convert), edge)
  }

  override def toString = edge match {
    case `[_,_)` => s"[${oToString(start)},${oToString(end)})"
    case `(_,_]` => s"(${oToString(start)},${oToString(end)}]"
    case `(_,_)` => s"(${oToString(start)},${oToString(end)})"
    case `[_,_]` => s"[${oToString(start)},${oToString(end)}]"
    case `empty` => Range.empty_str
  }
}

object Range {
  def emptyRange[T]: Range[T] = Range[T](None, None, `empty`)
  val empty_str = "empty"

  def apply[T](start: T, end: T, edge: EdgeType = `[_,_)`): Range[T] = Range(Some(start), Some(end), edge)
}

/**
 * simple range support; if all you want is just getting from / saving to db, and using pg range operations/methods, it should be enough
 */
trait PgRangeSupport extends range.PgRangeExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._

  private def toTimestamp(str: String) = Timestamp.valueOf(str)
  private def toSQLDate(str: String) = Date.valueOf(str)

  trait SimpleRangeCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("int4range", classTag[Range[Int]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("int8range", classTag[Range[Long]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("numrange", classTag[Range[Float]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("tsrange", classTag[Range[Timestamp]])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("daterange", classTag[Range[Date]])
    }
  }

  /// alias
  trait RangeImplicits extends SimpleRangeImplicits

  trait SimpleRangeImplicits extends SimpleRangeCodeGenSupport {
    implicit val simpleIntRangeTypeMapper: JdbcType[Range[Int]] = new GenericJdbcType[Range[Int]]("int4range", mkRangeFn(_.toInt))
    implicit val simpleLongRangeTypeMapper: JdbcType[Range[Long]] = new GenericJdbcType[Range[Long]]("int8range", mkRangeFn(_.toLong))
    implicit val simpleFloatRangeTypeMapper: JdbcType[Range[Float]] = new GenericJdbcType[Range[Float]]("numrange", mkRangeFn(_.toFloat))
    implicit val simpleTimestampRangeTypeMapper: JdbcType[Range[Timestamp]] = new GenericJdbcType[Range[Timestamp]]("tsrange", mkRangeFn(toTimestamp))
    implicit val simpleDateRangeTypeMapper: JdbcType[Range[Date]] = new GenericJdbcType[Range[Date]]("daterange", mkRangeFn(toSQLDate))

    implicit def simpleRangeColumnExtensionMethods[B0](c: Rep[Range[B0]])(
      implicit tm: JdbcType[B0], tm1: JdbcType[Range[B0]]) = {
        new RangeColumnExtensionMethods[Range[B0], B0, Range[B0]](c)
      }
    implicit def simpleRangeOptionColumnExtensionMethods[B0](c: Rep[Option[Range[B0]]])(
      implicit tm: JdbcType[B0], tm1: JdbcType[Range[B0]]) = {
        new RangeColumnExtensionMethods[Range[B0], B0, Option[Range[B0]]](c)
      }
  }

  trait SimpleRangePlainImplicits extends SimpleRangeCodeGenSupport {
    import utils.PlainSQLUtils._

    // to support 'nextArray[T]/nextArrayOption[T]' in PgArraySupport
    {
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(mkRangeFn(_.toInt))(r.nextString()))
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(mkRangeFn(_.toLong))(r.nextString()))
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(mkRangeFn(_.toFloat))(r.nextString()))
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(mkRangeFn(toTimestamp))(r.nextString()))
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(mkRangeFn(toSQLDate))(r.nextString()))
    }

    implicit class PgRangePositionedResult(r: PositionedResult) {
      def nextIntRange() = nextIntRangeOption().orNull
      def nextIntRangeOption() = r.nextStringOption().map(mkRangeFn(_.toInt))
      def nextLongRange() = nextLongRangeOption().orNull
      def nextLongRangeOption() = r.nextStringOption().map(mkRangeFn(_.toLong))
      def nextFloatRange() = nextFloatRangeOption().orNull
      def nextFloatRangeOption() = r.nextStringOption().map(mkRangeFn(_.toFloat))
      def nextTimestampRange() = nextTimestampRangeOption().orNull
      def nextTimestampRangeOption() = r.nextStringOption().map(mkRangeFn(toTimestamp))
      def nextDateRange() = nextDateRangeOption().orNull
      def nextDateRangeOption() = r.nextStringOption().map(mkRangeFn(toSQLDate))
    }

    ////////////////////////////////////////////////////////////////////
    implicit val getIntRange = mkGetResult(_.nextIntRange())
    implicit val getIntRangeOption = mkGetResult(_.nextIntRangeOption())
    implicit val setIntRange = mkSetParameter[Range[Int]]("int4range")
    implicit val setIntRangeOption = mkOptionSetParameter[Range[Int]]("int4range")

    implicit val getLongRange = mkGetResult(_.nextLongRange())
    implicit val getLongRangeOption = mkGetResult(_.nextLongRangeOption())
    implicit val setLongRange = mkSetParameter[Range[Long]]("int8range")
    implicit val setLongRangeOption = mkOptionSetParameter[Range[Long]]("int8range")

    implicit val getFloatRange = mkGetResult(_.nextFloatRange())
    implicit val getFloatRangeOption = mkGetResult(_.nextFloatRangeOption())
    implicit val setFloatRange = mkSetParameter[Range[Float]]("numrange")
    implicit val setFloatRangeOption = mkOptionSetParameter[Range[Float]]("numrange")

    implicit val getTimestampRange = mkGetResult(_.nextTimestamp())
    implicit val getTimestampRangeOption = mkGetResult(_.nextTimestampRangeOption())
    implicit val setTimestampRange = mkSetParameter[Range[Timestamp]]("tsrange")
    implicit val setTimestampRangeOption = mkOptionSetParameter[Range[Timestamp]]("tsrange")

    implicit val getDateRange = mkGetResult(_.nextDateRange())
    implicit val getDateRangeOption = mkGetResult(_.nextDateRangeOption())
    implicit val setDateRange = mkSetParameter[Range[Date]]("daterange")
    implicit val setDateRangeOption = mkOptionSetParameter[Range[Date]]("daterange")
  }
}

object PgRangeSupportUtils {

  // regular expr matchers to range string
  val `[_,_)Range`  = """\["?([^,"]*)"?,[ ]*"?([^,"]*)"?\)""".r   // matches: [_,_)
  val `(_,_]Range`  = """\("?([^,"]*)"?,[ ]*"?([^,"]*)"?\]""".r   // matches: (_,_]
  val `(_,_)Range`  = """\("?([^,"]*)"?,[ ]*"?([^,"]*)"?\)""".r   // matches: (_,_)
  val `[_,_]Range`  = """\["?([^,"]*)"?,[ ]*"?([^,"]*)"?\]""".r   // matches: [_,_]

  def mkRangeFn[T](convert: (String => T)): (String => Range[T]) = {
    def conv[T](str: String, convert: (String => T)): Option[T] =
      Option(str).filterNot(_.isEmpty).map(convert)

    (str: String) => str match {
      case Range.`empty_str` => Range.emptyRange[T]
      case `[_,_)Range`(start, end) => Range(conv(start, convert), conv(end, convert), `[_,_)`)
      case `(_,_]Range`(start, end) => Range(conv(start, convert), conv(end, convert), `(_,_]`)
      case `(_,_)Range`(start, end) => Range(conv(start, convert), conv(end, convert), `(_,_)`)
      case `[_,_]Range`(start, end) => Range(conv(start, convert), conv(end, convert), `[_,_]`)
    }
  }

  def toStringFn[T](toString: (T => String)): (Range[T] => String) =
    (r: Range[T]) => r.edge match {
      case `empty` => Range.empty_str
      case `[_,_)` => s"[${oToString(r.start, toString)},${oToString(r.end, toString)})"
      case `(_,_]` => s"(${oToString(r.start, toString)},${oToString(r.end, toString)}]"
      case `(_,_)` => s"(${oToString(r.start, toString)},${oToString(r.end, toString)})"
      case `[_,_]` => s"[${oToString(r.start, toString)},${oToString(r.end, toString)}]"
    }

  ///
  def mkWithLength[T](start: T, length: Double, edge: EdgeType = `[_,_)`) = {
    val upper = (start.asInstanceOf[Double] + length).asInstanceOf[T]
    new Range[T](Some(start), Some(upper), edge)
  }

  def mkWithInterval[T <: java.util.Date](start: T, interval: Interval, edge: EdgeType = `[_,_)`) = {
    val end = (start +: interval).asInstanceOf[T]
    new Range[T](Some(start), Some(end), edge)
  }

  ////// helper methods
  private[slickpg] def oToString[T](o: Option[T], toString: (T => String) = (r: T) => r.toString) =
    o.map(toString).getOrElse("")
}
