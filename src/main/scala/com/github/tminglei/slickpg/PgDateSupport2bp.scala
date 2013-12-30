package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import org.threeten.bp.{Duration, LocalDateTime, LocalTime, LocalDate}
import java.sql.{Timestamp, Time, Date}
import java.util.Calendar
import org.postgresql.util.PGInterval
import scala.slick.lifted.Column

trait PgDateSupport2bp extends date.PgDateExtensions with date.PgDateJavaTypes with utils.PgCommonJdbcTypes { driver: PostgresDriver =>

  type DATE   = LocalDate
  type TIME   = LocalTime
  type TIMESTAMP = LocalDateTime
  type INTERVAL  = Duration

  trait DateTimeImplicits {
    implicit val bpDateTypeMapper = new DateJdbcType(sqlDate2bpDate, bpDate2sqlDate)
    implicit val bpTimeTypeMapper = new TimeJdbcType(sqlTime2bpTime, bpTime2sqlTime)
    implicit val bpDateTimeTypeMapper = new TimestampJdbcType(sqlTimestamp2bpDateTime, bpDateTime2sqlTimestamp)
    implicit val bpDurationTypeMapper = new GenericJdbcType[Duration]("interval", pgIntervalStr2bpDuration)

    ///
    implicit def dateColumnExtensionMethods(c: Column[LocalDate]) = new DateColumnExtensionMethods(c)
    implicit def dateOptColumnExtensionMethods(c: Column[Option[LocalDate]]) = new DateColumnExtensionMethods(c)

    implicit def timeColumnExtensionMethods(c: Column[LocalTime]) = new TimeColumnExtensionMethods(c)
    implicit def timeOptColumnExtensionMethods(c: Column[Option[LocalTime]]) = new TimeColumnExtensionMethods(c)

    implicit def timestampColumnExtensionMethods(c: Column[LocalDateTime]) = new TimestampColumnExtensionMethods(c)
    implicit def timestampOptColumnExtensionMethods(c: Column[Option[LocalDateTime]]) = new TimestampColumnExtensionMethods(c)

    implicit def intervalColumnExtensionMethods(c: Column[Duration]) = new IntervalColumnExtensionMethods(c)
    implicit def intervalOptColumnExtensionMethods(c: Column[Option[Duration]]) = new IntervalColumnExtensionMethods(c)
  }

  //--------------------------------------------------------------------

  /// sql.Date <-> bp.LocalDate
  private def sqlDate2bpDate(date: Date): LocalDate = {
    val cal = Calendar.getInstance()
    cal.setTime(date)
    LocalDate.of(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH)
    )
  }
  private def bpDate2sqlDate(date: LocalDate): Date = {
    val cal = Calendar.getInstance()
    cal.set(date.getYear, date.getMonthValue, date.getDayOfMonth, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    new Date(cal.getTimeInMillis)
  }

  /// sql.Time <-> bp.LocalTime
  private def sqlTime2bpTime(time: Time): LocalTime = {
    val cal = Calendar.getInstance()
    cal.setTime(time)
    LocalTime.of(
      cal.get(Calendar.HOUR_OF_DAY),
      cal.get(Calendar.MINUTE),
      cal.get(Calendar.SECOND),
      cal.get(Calendar.MILLISECOND) * 1000
    )
  }
  private def bpTime2sqlTime(time: LocalTime): Time = {
    val cal = Calendar.getInstance()
    cal.set(0, 0, 0, time.getHour, time.getMinute, time.getSecond)
    cal.set(Calendar.MILLISECOND, time.getNano / 1000)
    new Time(cal.getTimeInMillis)
  }

  /// sql.Timestamp <-> bp.LocalDateTime
  private def sqlTimestamp2bpDateTime(ts: Timestamp): LocalDateTime = {
    val cal = Calendar.getInstance()
    cal.setTime(ts)
    LocalDateTime.of(
      cal.get(Calendar.YEAR),
      cal.get(Calendar.MONTH),
      cal.get(Calendar.DAY_OF_MONTH),
      cal.get(Calendar.HOUR_OF_DAY),
      cal.get(Calendar.MINUTE),
      cal.get(Calendar.SECOND),
      cal.get(Calendar.MILLISECOND) * 1000
    )
  }
  private def bpDateTime2sqlTimestamp(dt: LocalDateTime): Timestamp = {
    val cal = Calendar.getInstance()
    cal.set(dt.getYear, dt.getMonthValue, dt.getDayOfMonth, dt.getHour, dt.getMinute, dt.getSecond)
    cal.set(Calendar.MILLISECOND, dt.getNano / 1000)
    new Timestamp(cal.getTimeInMillis)
  }

  /// pg interval string <-> bp.Duration
  private def pgIntervalStr2bpDuration(intervalStr: String): Duration = {
    val pgInterval = new PGInterval(intervalStr)
    Duration.ofDays(pgInterval.getYears * 365 + pgInterval.getMonths * 30 + pgInterval.getDays)
      .plusHours(pgInterval.getHours)
      .plusMinutes(pgInterval.getMinutes)
      .plusMillis(Math.round(pgInterval.getSeconds * 1000))
  }
}
