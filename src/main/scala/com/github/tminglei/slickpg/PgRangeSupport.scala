package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import java.sql.{Date, Timestamp}
import slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}

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
        new RangeColumnExtensionMethods[Range, B0, Range[B0]](c)
      }
    implicit def simpleRangeOptionColumnExtensionMethods[B0](c: Rep[Option[Range[B0]]])(
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

    ////////////////////////////////////////////////////////////////////
    implicit object SetIntRange extends SetParameter[Range[Int]] {
      def apply(v: Range[Int], pp: PositionedParameters) = setRange("int4range", Option(v), pp)
    }
    implicit object SetIntRangeOption extends SetParameter[Option[Range[Int]]] {
      def apply(v: Option[Range[Int]], pp: PositionedParameters) = setRange("int4range", v, pp)
    }
    ///
    implicit object SetLongRange extends SetParameter[Range[Long]] {
      def apply(v: Range[Long], pp: PositionedParameters) = setRange("int8range", Option(v), pp)
    }
    implicit object SetLongRangeOption extends SetParameter[Option[Range[Long]]] {
      def apply(v: Option[Range[Long]], pp: PositionedParameters) = setRange("int8range", v, pp)
    }
    ///
    implicit object SetFloatRange extends SetParameter[Range[Float]] {
      def apply(v: Range[Float], pp: PositionedParameters) = setRange("numrange", Option(v), pp)
    }
    implicit object SetFloatRangeOption extends SetParameter[Option[Range[Float]]] {
      def apply(v: Option[Range[Float]], pp: PositionedParameters) = setRange("numrange", v, pp)
    }
    ///
    implicit object SetTimestampRange extends SetParameter[Range[Timestamp]] {
      def apply(v: Range[Timestamp], pp: PositionedParameters) = setRange("tsrange", Option(v), pp)
    }
    implicit object SetTimestampRangeOption extends SetParameter[Option[Range[Timestamp]]] {
      def apply(v: Option[Range[Timestamp]], pp: PositionedParameters) = setRange("tsrange", v, pp)
    }
    ///
    implicit object SetDateRange extends SetParameter[Range[Date]] {
      def apply(v: Range[Date], pp: PositionedParameters) = setRange("daterange", Option(v), pp)
    }
    implicit object SetDateRangeOption extends SetParameter[Option[Range[Date]]] {
      def apply(v: Option[Range[Date]], pp: PositionedParameters) = setRange("daterange", v, pp)
    }

    ///
    private def setRange[T](typeName: String, v: Option[Range[T]], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject(typeName, v.toString), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
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
