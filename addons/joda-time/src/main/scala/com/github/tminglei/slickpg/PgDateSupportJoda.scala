package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import org.joda.time._
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormat}
import scala.slick.lifted.Column
import org.postgresql.util.PGInterval

trait PgDateSupportJoda extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import PgJodaSupportUtils._

  type DATE   = LocalDate
  type TIME   = LocalTime
  type TIMESTAMP = LocalDateTime
  type INTERVAL  = Period

  type TIMESTAMP_TZ = DateTime

  trait DateTimeImplicits {
    val dateFormatter = ISODateTimeFormat.date()
    val timeFormatter = DateTimeFormat.forPattern("HH:mm:ss.SSSSSS")
    val timeFormatter_NoFraction = DateTimeFormat.forPattern("HH:mm:ss")
    val dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
    val dateTimeFormatter_NoFraction = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val tzDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")
    val tzDateTimeFormatter_NoFraction = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ")

    implicit val jodaDateTypeMapper = new GenericJdbcType[LocalDate]("date",
      LocalDate.parse(_, dateFormatter), _.toString(dateFormatter), hasLiteralForm=false)
    implicit val jodaTimeTypeMapper = new GenericJdbcType[LocalTime]("time",
      fnFromString = (s) => LocalTime.parse(s, if(s.indexOf(".") > 0 ) timeFormatter else timeFormatter_NoFraction),
      fnToString = (v) => v.toString(timeFormatter),
      hasLiteralForm = false)
    implicit val jodaDateTimeTypeMapper = new GenericJdbcType[LocalDateTime]("timestamp",
      fnFromString = (s) => LocalDateTime.parse(s, if(s.indexOf(".") > 0 ) dateTimeFormatter else dateTimeFormatter_NoFraction),
      fnToString = (v) => v.toString(dateTimeFormatter),
      hasLiteralForm = false)
    implicit val jodaPeriodTypeMapper = new GenericJdbcType[Period]("interval",
      pgIntervalStr2jodaPeriod, hasLiteralForm=false)
    implicit val timestampTZTypeMapper = new GenericJdbcType[DateTime]("timestamptz",
      fnFromString = (s) => DateTime.parse(s, if(s.indexOf(".") > 0 ) tzDateTimeFormatter else tzDateTimeFormatter_NoFraction),
      fnToString = (v) => v.toString(tzDateTimeFormatter),
      hasLiteralForm = false)

    ///
    implicit def dateColumnExtensionMethods(c: Column[LocalDate]) = new DateColumnExtensionMethods(c)
    implicit def dateOptColumnExtensionMethods(c: Column[Option[LocalDate]]) = new DateColumnExtensionMethods(c)

    implicit def timeColumnExtensionMethods(c: Column[LocalTime]) = new TimeColumnExtensionMethods(c)
    implicit def timeOptColumnExtensionMethods(c: Column[Option[LocalTime]]) = new TimeColumnExtensionMethods(c)

    implicit def timestampColumnExtensionMethods(c: Column[LocalDateTime]) = new TimestampColumnExtensionMethods(c)
    implicit def timestampOptColumnExtensionMethods(c: Column[Option[LocalDateTime]]) = new TimestampColumnExtensionMethods(c)

    implicit def intervalColumnExtensionMethods(c: Column[Period]) = new IntervalColumnExtensionMethods(c)
    implicit def intervalOptColumnExtensionMethods(c: Column[Option[Period]]) = new IntervalColumnExtensionMethods(c)

    implicit def timestampTZColumnExtensionMethods(c: Column[DateTime]) = new TimestampTZColumnExtensionMethods(c)
    implicit def timestampTZOptColumnExtensionMethods(c: Column[Option[DateTime]]) = new TimestampTZColumnExtensionMethods(c)
  }
}

object PgJodaSupportUtils {
  /// pg interval string <-> joda Duration
  def pgIntervalStr2jodaPeriod(intervalStr: String): Period = {
    val pgInterval = new PGInterval(intervalStr)
    val seconds = Math.floor(pgInterval.getSeconds) .asInstanceOf[Int]
    val millis  = ((pgInterval.getSeconds - seconds) * 1000) .asInstanceOf[Int]

    new Period(
      pgInterval.getYears,
      pgInterval.getMonths,
      0,  // weeks
      pgInterval.getDays,
      pgInterval.getHours,
      pgInterval.getMinutes,
      seconds, millis
    )
  }
}