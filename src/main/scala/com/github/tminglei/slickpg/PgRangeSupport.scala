package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import java.sql.{Date, Timestamp}
import scala.slick.jdbc.JdbcType

trait PgRangeSupport extends range.PgRangeExtensions { driver: PostgresDriver =>

  type RANGEType[T] = Range[T]

  private val tsFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  private val dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd")
  private def toTimestamp(str: String) = new Timestamp(tsFormatter.parse(str).getTime)
  private def toSQLDate(str: String) = new Date(dateFormatter.parse(str).getTime)

  trait RangeImplicits {
    implicit val intRangeTypeMapper = new utils.GenericJdbcType[Range[Int]]("int4range", Range.mkRangeFn(_.toInt))
    implicit val longRangeTypeMapper = new utils.GenericJdbcType[Range[Long]]("int8range", Range.mkRangeFn(_.toLong))
    implicit val floatRangeTypeMapper = new utils.GenericJdbcType[Range[Float]]("numrange", Range.mkRangeFn(_.toFloat))
    implicit val timestampRangeTypeMapper = new utils.GenericJdbcType[Range[Timestamp]]("tsrange", Range.mkRangeFn(toTimestamp))
    implicit val dateRangeTypeMapper = new utils.GenericJdbcType[Range[Date]]("daterange", Range.mkRangeFn(toSQLDate))

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
