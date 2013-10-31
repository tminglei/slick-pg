package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted._
import java.sql.{Timestamp, Time, Date}

trait PgDateSupport extends date.PgDateExtensions {driver: PostgresDriver =>

  type DATE   = Date
  type TIME   = Time
  type TIMESTAMP = Timestamp
  type INTERVAL  = Interval

  trait DatetimeImplicits {
    implicit val intervalTypeMapper = new utils.GenericTypeMapper[Interval]("interval", Interval.apply)

    ///
    implicit def dateColumnExtensionMethods(c: Column[Date]) = new DateColumnExtensionMethods(c)
    implicit def dateOptColumnExtensionMethods(c: Column[Option[Date]]) = new DateColumnExtensionMethods(c)

    implicit def timeColumnExtensionMethods(c: Column[Time]) = new TimeColumnExtensionMethods(c)
    implicit def timeOptColumnExtensionMethods(c: Column[Option[Time]]) = new TimeColumnExtensionMethods(c)

    implicit def timestampColumnExtensionMethods(c: Column[Timestamp]) = new TimestampColumnExtensionMethods(c)
    implicit def timestampOptColumnExtensionMethods(c: Column[Option[Timestamp]]) = new TimestampColumnExtensionMethods(c)

    implicit def intervalColumnExtensionMethods(c: Column[Interval]) = new IntervalColumnExtensionMethods(c)
    implicit def intervalOptColumnExtensionMethods(c: Column[Option[Interval]]) = new IntervalColumnExtensionMethods(c)
  }

}
