package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.{TypeMapper, Column}
import java.sql.{Date, Timestamp}

trait PgRangeSupport extends range.PgRangeExtensions { driver: PostgresDriver =>

  type RANGEType[T] = Range[T]

  private val tsFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  private val dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd")
  private def toTimestamp(str: String) = new Timestamp(tsFormatter.parse(str).getTime)
  private def toSQLDate(str: String) = new Date(dateFormatter.parse(str).getTime)

  trait RangeImplicits {
    implicit val intRangeTypeMapper = new utils.GenericTypeMapper[Range[Int]]("int4range", Range.mkRangeFn(_.toInt))
    implicit val longRangeTypeMapper = new utils.GenericTypeMapper[Range[Long]]("int8range", Range.mkRangeFn(_.toLong))
    implicit val floatRangeTypeMapper = new utils.GenericTypeMapper[Range[Float]]("numrange", Range.mkRangeFn(_.toFloat))
    implicit val timestampRangeTypeMapper = new utils.GenericTypeMapper[Range[Timestamp]]("tsrange", Range.mkRangeFn(toTimestamp))
    implicit val dateRangeTypeMapper = new utils.GenericTypeMapper[Range[Date]]("daterange", Range.mkRangeFn(toSQLDate))

    implicit def rangeColumnExtensionMethods[B0, Range[B0]](c: Column[Range[B0]])(
      implicit tm: TypeMapper[B0], tm1: TypeMapper[RANGEType[B0]]) = {
        new RangeColumnExtensionMethods[B0, Range[B0]](c)
      }
    implicit def rangeOptionColumnExtensionMethods[B0, Range[B0]](c: Column[Option[Range[B0]]])(
      implicit tm: TypeMapper[B0], tm1: TypeMapper[RANGEType[B0]]) = {
        new RangeColumnExtensionMethods[B0, Option[Range[B0]]](c)
      }
  }
}
