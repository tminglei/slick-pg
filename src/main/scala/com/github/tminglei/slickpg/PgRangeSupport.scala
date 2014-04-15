package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import java.sql.{Date, Timestamp}
import com.github.tminglei.slickpg.PgRangeSupportUtils.{tsFormatter, dateFormatter}

trait PgRangeSupport extends range.PgRangeExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>

  type RANGEType[T] = Range[T]

  private def toTimestamp(str: String) = new Timestamp(tsFormatter().parse(str).getTime)
  private def toSQLDate(str: String) = new Date(dateFormatter().parse(str).getTime)

  trait RangeImplicits {
    implicit val intRangeTypeMapper = new GenericJdbcType[Range[Int]]("int4range", PgRangeSupportUtils.mkRangeFn(_.toInt))
    implicit val longRangeTypeMapper = new GenericJdbcType[Range[Long]]("int8range", PgRangeSupportUtils.mkRangeFn(_.toLong))
    implicit val floatRangeTypeMapper = new GenericJdbcType[Range[Float]]("numrange", PgRangeSupportUtils.mkRangeFn(_.toFloat))
    implicit val timestampRangeTypeMapper = new GenericJdbcType[Range[Timestamp]]("tsrange", PgRangeSupportUtils.mkRangeFn(toTimestamp))
    implicit val dateRangeTypeMapper = new GenericJdbcType[Range[Date]]("daterange", PgRangeSupportUtils.mkRangeFn(toSQLDate))

    implicit def rangeColumnExtensionMethods[B0, Range[B0]](c: Column[Range[B0]])(
      implicit tm: JdbcType[B0], tm1: JdbcType[RANGEType[B0]]) = {
        new RangeColumnExtensionMethods[B0, Range[B0]](c)
      }
    implicit def rangeOptionColumnExtensionMethods[B0, Range[B0]](c: Column[Option[Range[B0]]])(
      implicit tm: JdbcType[B0], tm1: JdbcType[RANGEType[B0]]) = {
        new RangeColumnExtensionMethods[B0, Option[Range[B0]]](c)
      }
  }
}
