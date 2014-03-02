package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import org.joda.time._
import org.joda.time.format.DateTimeFormat
import java.sql.{Timestamp, Time, Date}
import scala.slick.lifted.Column
import org.postgresql.util.PGInterval

trait PgDateSupportJoda extends date.PgDateExtensions with date.PgDateJavaTypes with utils.PgCommonJdbcTypes { driver: PostgresDriver =>

  type DATE   = LocalDate
  type TIME   = LocalTime
  type TIMESTAMP = LocalDateTime
  type INTERVAL  = Period

  type TIMESTAMP_TZ = DateTime

  trait DateTimeImplicits {
    val tzDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ")

    implicit val jodaDateTypeMapper = new DateJdbcType(sqlDate2jodaDate, jodaDate2sqlDate)
    implicit val jodaTimeTypeMapper = new TimeJdbcType(sqlTime2jodaTime, jodaTime2sqlTime)
    implicit val jodaDateTimeTypeMapper = new TimestampJdbcType(sqlTimestamp2jodaDateTime, jodaDateTime2sqlTimestamp)
    implicit val jodaPeriodTypeMapper = new GenericJdbcType[Period]("interval", pgIntervalStr2jodaPeriod, hasLiteralForm=false)
    implicit val timestampTZTypeMapper = new GenericJdbcType[DateTime]("timestamptz",
        DateTime.parse(_, tzDateTimeFormatter), _.toString(tzDateTimeFormatter), hasLiteralForm=false)

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

  //--------------------------------------------------------------------

  /// sql.Date <-> joda LocalDate
  private def sqlDate2jodaDate(date: Date): LocalDate = {
    new LocalDate(date)
  }

  private def jodaDate2sqlDate(date: LocalDate): Date = {
    new Date(date.toDateTimeAtStartOfDay().toDate().getTime())
  }

  /// sql.Time <-> joda LocalTime
  private def sqlTime2jodaTime(time: Time): LocalTime = {
    new LocalTime(time, DateTimeZone.UTC)
  }

  private def jodaTime2sqlTime(time: LocalTime): Time = {
    new Time(time.getMillisOfDay)
  }

  /// sql.Timestamp <-> joda LocalDateTime
  private def sqlTimestamp2jodaDateTime(ts: Timestamp): LocalDateTime = {
    new LocalDateTime(ts)
  }

  private def jodaDateTime2sqlTimestamp(dt: LocalDateTime): Timestamp = {
    new Timestamp(dt.toDateTime.toDate.getTime)
  }

  /// pg interval string <-> joda Duration
  private def pgIntervalStr2jodaPeriod(intervalStr: String): Period = {
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
