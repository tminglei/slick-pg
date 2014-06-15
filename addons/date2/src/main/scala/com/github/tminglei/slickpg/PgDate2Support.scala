package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import java.time.{Duration, LocalDateTime, LocalTime, LocalDate, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.sql.{Timestamp, Time, Date}
import java.util.Calendar
import org.postgresql.util.PGInterval
import scala.slick.lifted.Column

trait PgDate2Support extends date.PgDateExtensions with date.PgDateJdbcTypes with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import PgDate2SupportUtils._

  type DATE   = LocalDate
  type TIME   = LocalTime
  type TIMESTAMP = LocalDateTime
  type INTERVAL  = Duration

  type TIMESTAMP_TZ = ZonedDateTime

  trait DateTimeImplicits {
    val tzDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]X")

    implicit val bpDateTypeMapper = new DateJdbcType(sqlDate2LocalDate, localDate2sqlDate)
    implicit val bpTimeTypeMapper = new TimeJdbcType(sqlTime2LocalTime, localTime2sqlTime)
    implicit val bpDateTimeTypeMapper = new TimestampJdbcType(sqlTimestamp2LocalDateTime, localDateTime2sqlTimestamp)
    implicit val bpDurationTypeMapper = new GenericJdbcType[Duration]("interval", pgIntervalStr2Duration, hasLiteralForm=false)
    implicit val timestampTZTypeMapper = new GenericJdbcType[ZonedDateTime]("timestamptz",
      ZonedDateTime.parse(_, tzDateTimeFormatter), _.format(tzDateTimeFormatter), hasLiteralForm=false)

    ///
    implicit def dateColumnExtensionMethods(c: Column[LocalDate]) = new DateColumnExtensionMethods(c)
    implicit def dateOptColumnExtensionMethods(c: Column[Option[LocalDate]]) = new DateColumnExtensionMethods(c)

    implicit def timeColumnExtensionMethods(c: Column[LocalTime]) = new TimeColumnExtensionMethods(c)
    implicit def timeOptColumnExtensionMethods(c: Column[Option[LocalTime]]) = new TimeColumnExtensionMethods(c)

    implicit def timestampColumnExtensionMethods(c: Column[LocalDateTime]) = new TimestampColumnExtensionMethods(c)
    implicit def timestampOptColumnExtensionMethods(c: Column[Option[LocalDateTime]]) = new TimestampColumnExtensionMethods(c)

    implicit def intervalColumnExtensionMethods(c: Column[Duration]) = new IntervalColumnExtensionMethods(c)
    implicit def intervalOptColumnExtensionMethods(c: Column[Option[Duration]]) = new IntervalColumnExtensionMethods(c)

    implicit def timestampTZColumnExtensionMethods(c: Column[ZonedDateTime]) = new TimestampTZColumnExtensionMethods(c)
    implicit def timestampTZOptColumnExtensionMethods(c: Column[Option[ZonedDateTime]]) = new TimestampTZColumnExtensionMethods(c)
  }
}

object PgDate2SupportUtils {
  /// sql.Date <-> time.LocalDate
  def sqlDate2LocalDate(date: Date): LocalDate = {
    val cal = Calendar.getInstance()
    cal.setTime(date)
    LocalDate.of(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) +1,
      cal.get(Calendar.DAY_OF_MONTH)
    )
  }
  def localDate2sqlDate(date: LocalDate): Date = {
    val cal = Calendar.getInstance()
    cal.set(date.getYear, date.getMonthValue -1, date.getDayOfMonth, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    new Date(cal.getTimeInMillis)
  }

  /// sql.Time <-> time.LocalTime
  def sqlTime2LocalTime(time: Time): LocalTime = {
    val cal = Calendar.getInstance()
    cal.setTime(time)
    LocalTime.of(
      cal.get(Calendar.HOUR_OF_DAY),
      cal.get(Calendar.MINUTE),
      cal.get(Calendar.SECOND),
      cal.get(Calendar.MILLISECOND) * 1000000
    )
  }
  def localTime2sqlTime(time: LocalTime): Time = {
    val cal = Calendar.getInstance()
    cal.set(0, 0, 0, time.getHour, time.getMinute, time.getSecond)
    cal.set(Calendar.MILLISECOND, time.getNano / 1000000)
    new Time(cal.getTimeInMillis)
  }

  /// sql.Timestamp <-> time.LocalDateTime
  def sqlTimestamp2LocalDateTime(ts: Timestamp): LocalDateTime = {
    val cal = Calendar.getInstance()
    cal.setTime(ts)
    LocalDateTime.of(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH) +1,
      cal.get(Calendar.DAY_OF_MONTH),
      cal.get(Calendar.HOUR_OF_DAY),
      cal.get(Calendar.MINUTE),
      cal.get(Calendar.SECOND),
      cal.get(Calendar.MILLISECOND) * 1000000
    )
  }
  def localDateTime2sqlTimestamp(ts: LocalDateTime): Timestamp = {
    val cal = Calendar.getInstance()
    cal.set(ts.getYear, ts.getMonthValue -1, ts.getDayOfMonth, ts.getHour, ts.getMinute, ts.getSecond)
    cal.set(Calendar.MILLISECOND, ts.getNano / 1000000)
    new Timestamp(cal.getTimeInMillis)
  }

  /// pg interval string <-> time.Duration
  def pgIntervalStr2Duration(intervalStr: String): Duration = {
    val pgInterval = new PGInterval(intervalStr)
    Duration.ofDays(pgInterval.getYears * 365 + pgInterval.getMonths * 30 + pgInterval.getDays)
      .plusHours(pgInterval.getHours)
      .plusMinutes(pgInterval.getMinutes)
      .plusMillis(Math.round(pgInterval.getSeconds * 1000))
  }
}
