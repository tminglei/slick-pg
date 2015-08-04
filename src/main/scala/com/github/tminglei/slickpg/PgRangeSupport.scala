package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import java.sql.{Date, Timestamp}
import slick.jdbc.{PositionedResult, JdbcType}

// edge type definitions
sealed trait EdgeType
case object `[_,_)` extends EdgeType
case object `(_,_]` extends EdgeType
case object `(_,_)` extends EdgeType
case object `[_,_]` extends EdgeType

case class Range[T](start: T, end: T, edge: EdgeType = `[_,_)`) {

  def as[A](convert: (T => A)): Range[A] = {
    new Range[A](convert(start), convert(end), edge)
  }

  override def toString = edge match {
    case `[_,_)` => s"[$start,$end)"
    case `(_,_]` => s"($start,$end]"
    case `(_,_)` => s"($start,$end)"
    case `[_,_]` => s"[$start,$end]"
  }
}

/**
 * simple range support; if all you want is just getting from / saving to db, and using pg range operations/methods, it should be enough
 */
trait PgRangeSupport extends range.PgRangeExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import PgRangeSupportUtils._

  private def toTimestamp(str: String) = Timestamp.valueOf(str)
  private def toSQLDate(str: String) = Date.valueOf(str)

  /// alias
  trait RangeImplicits extends SimpleRangeImplicits

  trait SimpleRangeImplicits {
    implicit val simpleIntRangeTypeMapper = new GenericJdbcType[Range[Int]]("int4range", mkRangeFn(_.toInt))
    implicit val simpleLongRangeTypeMapper = new GenericJdbcType[Range[Long]]("int8range", mkRangeFn(_.toLong))
    implicit val simpleFloatRangeTypeMapper = new GenericJdbcType[Range[Float]]("numrange", mkRangeFn(_.toFloat))
    implicit val simpleTimestampRangeTypeMapper = new GenericJdbcType[Range[Timestamp]]("tsrange", mkRangeFn(toTimestamp))
    implicit val simpleDateRangeTypeMapper = new GenericJdbcType[Range[Date]]("daterange", mkRangeFn(toSQLDate))

    implicit def simpleRangeColumnExtensionMethods[B0](c: Rep[Range[B0]])(
      implicit tm: JdbcType[B0], tm1: JdbcType[Range[B0]]) = {
        new RangeColumnExtensionMethods[Range[B0], B0, Range[B0]](c)
      }
    implicit def simpleRangeOptionColumnExtensionMethods[B0](c: Rep[Option[Range[B0]]])(
      implicit tm: JdbcType[B0], tm1: JdbcType[Range[B0]]) = {
        new RangeColumnExtensionMethods[Range[B0], B0, Option[Range[B0]]](c)
      }
  }

  trait SimpleRangePlainImplicits {
    import scala.reflect.classTag
    import utils.PlainSQLUtils._
    {
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(mkRangeFn(_.toInt))(r.nextString()))
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(mkRangeFn(_.toLong))(r.nextString()))
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(mkRangeFn(_.toFloat))(r.nextString()))
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(mkRangeFn(toTimestamp))(r.nextString()))
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(mkRangeFn(toSQLDate))(r.nextString()))
    }

    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("int4range", classTag[Range[Int]])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("int8range", classTag[Range[Long]])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("numrange", classTag[Range[Float]])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("tsrange", classTag[Range[Timestamp]])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("daterange", classTag[Range[Date]])
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
      def nextDateRangeOption() = r.nextStringOption().map(toSQLDate)
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

  def mkRangeFn[T](convert: (String => T)): (String => Range[T]) =
    (str: String) => str match {
      case `[_,_)Range`(start, end) => Range(convert(start), convert(end), `[_,_)`)
      case `(_,_]Range`(start, end) => Range(convert(start), convert(end), `(_,_]`)
      case `(_,_)Range`(start, end) => Range(convert(start), convert(end), `(_,_)`)
      case `[_,_]Range`(start, end) => Range(convert(start), convert(end), `[_,_]`)
    }

  def toStringFn[T](toString: (T => String)): (Range[T] => String) =
    (r: Range[T]) => r.edge match {
      case `[_,_)` => s"[${toString(r.start)},${toString(r.end)})"
      case `(_,_]` => s"(${toString(r.start)},${toString(r.end)}]"
      case `(_,_)` => s"(${toString(r.start)},${toString(r.end)})"
      case `[_,_]` => s"[${toString(r.start)},${toString(r.end)}]"
    }

  ///
  def mkWithLength[T](start: T, length: Double, edge: EdgeType = `[_,_)`) = {
    val upper = (start.asInstanceOf[Double] + length).asInstanceOf[T]
    new Range[T](start, upper, edge)
  }

  def mkWithInterval[T <: java.util.Date](start: T, interval: Interval, edge: EdgeType = `[_,_)`) = {
    val end = (start +: interval).asInstanceOf[T]
    new Range[T](start, end, edge)
  }
}
