package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import java.sql.{Date, Timestamp}
import scala.slick.jdbc.JdbcType

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

  trait RangeImplicits {
    implicit val intRangeTypeMapper = new GenericJdbcType[Range[Int]]("int4range", mkRangeFn(_.toInt))
    implicit val longRangeTypeMapper = new GenericJdbcType[Range[Long]]("int8range", mkRangeFn(_.toLong))
    implicit val floatRangeTypeMapper = new GenericJdbcType[Range[Float]]("numrange", mkRangeFn(_.toFloat))
    implicit val timestampRangeTypeMapper = new GenericJdbcType[Range[Timestamp]]("tsrange", mkRangeFn(toTimestamp))
    implicit val dateRangeTypeMapper = new GenericJdbcType[Range[Date]]("daterange", mkRangeFn(toSQLDate))

    implicit def rangeColumnExtensionMethods[B0](c: Column[Range[B0]])(
      implicit tm: JdbcType[B0], tm1: JdbcType[Range[B0]]) = {
        new RangeColumnExtensionMethods[Range, B0, Range[B0]](c)
      }
    implicit def rangeOptionColumnExtensionMethods[B0](c: Column[Option[Range[B0]]])(
      implicit tm: JdbcType[B0], tm1: JdbcType[Range[B0]]) = {
        new RangeColumnExtensionMethods[Range, B0, Option[Range[B0]]](c)
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