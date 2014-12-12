package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import java.sql.{Date, Timestamp}
import scala.slick.jdbc.{PositionedParameters, PositionedResult, JdbcType}

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

    implicit def simpleRangeColumnExtensionMethods[B0](c: Column[Range[B0]])(
      implicit tm: JdbcType[B0], tm1: JdbcType[Range[B0]]) = {
        new RangeColumnExtensionMethods[Range, B0, Range[B0]](c)
      }
    implicit def simpleRangeOptionColumnExtensionMethods[B0](c: Column[Option[Range[B0]]])(
      implicit tm: JdbcType[B0], tm1: JdbcType[Range[B0]]) = {
        new RangeColumnExtensionMethods[Range, B0, Option[Range[B0]]](c)
      }
  }

  trait SimpleRangePlainImplicits {
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
    implicit class PgRangePositionedParameters(p: PositionedParameters) {
      def setIntRange(v: Range[Int]) = setIntRangeOption(Option(v))
      def setIntRangeOption(v: Option[Range[Int]]) = setRange[Int]("int4range", v)
      def setLongRange(v: Range[Long]) = setLongRangeOption(Option(v))
      def setLongRangeOption(v: Option[Range[Long]]) = setRange[Long]("int8range", v)
      def setFloatRange(v: Range[Float]) = setFloatRangeOption(Option(v))
      def setFloatRangeOption(v: Option[Range[Float]]) = setRange[Float]("numrange", v)
      def setTimestampRange(v: Range[Timestamp]) = setTimestampRangeOption(Option(v))
      def setTimestampRangeOption(v: Option[Range[Timestamp]]) = setRange[Timestamp]("tsrange", v)
      def setDateRange(v: Range[Date]) = setDateRangeOption(Option(v))
      def setDateRangeOption(v: Option[Range[Date]]) = setRange[Date]("daterange", v)
      ///
      private def setRange[T](typeName: String, v: Option[Range[T]]) = {
        p.pos += 1
        v match {
          case Some(v) => p.ps.setObject(p.pos, utils.mkPGobject(typeName, v.toString))
          case None    => p.ps.setNull(p.pos, java.sql.Types.OTHER)
        }
      }
    }
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