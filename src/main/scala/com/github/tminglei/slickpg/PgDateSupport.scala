package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import java.sql.{Timestamp, Time, Date}
import javax.xml.bind.DatatypeConverter
import scala.slick.lifted.Column
import java.util.Calendar

trait PgDateSupport extends date.PgDateExtensions with date.PgDateJavaTypes with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.Implicit._

  type DATE   = Date
  type TIME   = Time
  type TIMESTAMP = Timestamp
  type INTERVAL  = Interval
  
  type TIMESTAMP_TZ = Calendar

  trait DateTimeImplicits {
    implicit val intervalTypeMapper = new GenericJdbcType[Interval]("interval", Interval.apply, hasLiteralForm=false)
    implicit val timestampTZTypeMapper = new GenericJdbcType[Calendar]("timestamptz",
        date.PgDateJavaTypeUtils.parseCalendar, DatatypeConverter.printDateTime, hasLiteralForm=false)

    ///
    implicit def dateColumnExtensionMethods(c: Column[Date]) = new DateColumnExtensionMethods(c)
    implicit def dateOptColumnExtensionMethods(c: Column[Option[Date]]) = new DateColumnExtensionMethods(c)

    implicit def timeColumnExtensionMethods(c: Column[Time]) = new TimeColumnExtensionMethods(c)
    implicit def timeOptColumnExtensionMethods(c: Column[Option[Time]]) = new TimeColumnExtensionMethods(c)

    implicit def timestampColumnExtensionMethods(c: Column[Timestamp]) = new TimestampColumnExtensionMethods(c)
    implicit def timestampOptColumnExtensionMethods(c: Column[Option[Timestamp]]) = new TimestampColumnExtensionMethods(c)

    implicit def intervalColumnExtensionMethods(c: Column[Interval]) = new IntervalColumnExtensionMethods(c)
    implicit def intervalOptColumnExtensionMethods(c: Column[Option[Interval]]) = new IntervalColumnExtensionMethods(c)

    implicit def timestampTZColumnExtensionMethods(c: Column[Calendar]) = new TimestampTZColumnExtensionMethods(c)
    implicit def timestampTZOptColumnExtensionMethods(c: Column[Option[Calendar]]) = new TimestampTZColumnExtensionMethods(c)
  }
}
