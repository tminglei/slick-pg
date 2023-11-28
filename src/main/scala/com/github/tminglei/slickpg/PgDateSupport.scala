package com.github.tminglei.slickpg

import java.sql.{Date, Time, Timestamp}
import java.util.Calendar

import slick.jdbc.{JdbcType, PostgresProfile}

trait PgDateSupport extends date.PgDateExtensions with utils.PgCommonJdbcTypes with date.PgDateJdbcTypes { driver: PostgresProfile =>
  import driver.api._

  trait SimpleDateTimeImplicits {
    implicit val simpleIntervalTypeMapper: JdbcType[Interval] = new GenericJdbcType[Interval]("interval", Interval.apply, hasLiteralForm=false)
    implicit val simpleTimestampTZTypeMapper: JdbcType[Calendar] = new GenericDateJdbcType[Calendar]("timestamptz", java.sql.Types.TIMESTAMP_WITH_TIMEZONE)

    ///
    implicit def simpleDateColumnExtensionMethods(c: Rep[Date]): DateColumnExtensionMethods[Date, Time, Timestamp, Interval, Date] =
      new DateColumnExtensionMethods[Date, Time, Timestamp,  Interval, Date](c)
    implicit def simpleDateOptColumnExtensionMethods(c: Rep[Option[Date]]): DateColumnExtensionMethods[Date, Time, Timestamp, Interval, Option[Date]] =
      new DateColumnExtensionMethods[Date, Time, Timestamp, Interval, Option[Date]](c)

    implicit def simpleTimeColumnExtensionMethods(c: Rep[Time]): TimeColumnExtensionMethods[Date, Time, Timestamp, Time, Interval, Time] =
      new TimeColumnExtensionMethods[Date, Time, Timestamp, Time, Interval, Time](c)
    implicit def simpleTimeOptColumnExtensionMethods(c: Rep[Option[Time]]): TimeColumnExtensionMethods[Date, Time, Timestamp, Time, Interval, Option[Time]] =
      new TimeColumnExtensionMethods[Date, Time, Timestamp, Time, Interval, Option[Time]](c)

    implicit def simpleTimestampColumnExtensionMethods(c: Rep[Timestamp]): TimestampColumnExtensionMethods[Date, Time, Timestamp, Calendar, Interval, Timestamp] =
      new TimestampColumnExtensionMethods[Date, Time, Timestamp, Calendar, Interval, Timestamp](c)
    implicit def simpleTimestampOptColumnExtensionMethods(c: Rep[Option[Timestamp]]): TimestampColumnExtensionMethods[Date, Time, Timestamp, Calendar, Interval, Option[Timestamp]] =
      new TimestampColumnExtensionMethods[Date, Time, Timestamp, Calendar, Interval, Option[Timestamp]](c)

    implicit def simpleIntervalColumnExtensionMethods(c: Rep[Interval]): IntervalColumnExtensionMethods[Date, Time, Timestamp, Interval, Interval] =
      new IntervalColumnExtensionMethods[Date, Time, Timestamp, Interval, Interval](c)
    implicit def simpleIntervalOptColumnExtensionMethods(c: Rep[Option[Interval]]): IntervalColumnExtensionMethods[Date, Time, Timestamp, Interval, Option[Interval]] =
      new IntervalColumnExtensionMethods[Date, Time, Timestamp, Interval, Option[Interval]](c)

    implicit def simpleTimestampTZColumnExtensionMethods(c: Rep[Calendar]): TimestampColumnExtensionMethods[Date, Time, Calendar, Calendar, Interval, Calendar] =
      new TimestampColumnExtensionMethods[Date, Time, Calendar, Calendar, Interval, Calendar](c)
    implicit def simpleTimestampTZOptColumnExtensionMethods(c: Rep[Option[Calendar]]): TimestampColumnExtensionMethods[Date, Time, Calendar, Calendar, Interval, Option[Calendar]] =
      new TimestampColumnExtensionMethods[Date, Time, Calendar, Calendar, Interval, Option[Calendar]](c)
  }
}

/**
 * copy from [[org.postgresql.util.PGInterval]],
 * should be more convenient to be used in scala environment
 */
import java.text.DecimalFormat
import org.postgresql.util.PGInterval

case class Interval(
  years: Int,
  months: Int,
  days: Int,
  hours: Int,
  minutes: Int,
  seconds: Double) {

  def milliseconds: Int = (microseconds + (if (microseconds < 0) -500 else 500)) / 1000
  def microseconds: Int = (seconds * 1000000.0).asInstanceOf[Int]

  def +:(cal: Calendar): Calendar = {
    cal.add(Calendar.MILLISECOND, milliseconds)
    cal.add(Calendar.MINUTE, minutes)
    cal.add(Calendar.HOUR, hours)
    cal.add(Calendar.DAY_OF_MONTH, days)
    cal.add(Calendar.MONTH, months)
    cal.add(Calendar.YEAR, years)
    cal
  }

  def +:(date: java.util.Date): java.util.Date = {
    val cal = Calendar.getInstance
    cal.setTime(date)
    date.setTime((cal +: this).getTime.getTime)
    date
  }

  def +(other: Interval): Interval = {
    new Interval(
      years + other.years,
      months + other.months,
      days + other.days,
      hours + other.hours,
      minutes + other.minutes,
      seconds + other.seconds
    )
  }

  def *(factor: Int): Interval = {
    new Interval(
      years * factor,
      months * factor,
      days * factor,
      hours * factor,
      minutes * factor,
      seconds * factor
    )
  }

  override def toString = {
    val secs = Interval.secondsFormat.format(seconds)
    ""+years+" years "+months+" mons "+days+" days "+hours+" hours "+minutes+" mins "+secs+" secs"
  }
}

object Interval {
  private val secondsFormat = {
    val format = new DecimalFormat("0.00####")
    val dfs = format.getDecimalFormatSymbols()
    dfs.setDecimalSeparator('.')
    format.setDecimalFormatSymbols(dfs)
    format
  }

  def apply(interval: String): Interval = fromPgInterval(new PGInterval(interval))

  def fromPgInterval(interval: PGInterval): Interval = {
    new Interval(
      interval.getYears,
      interval.getMonths,
      interval.getDays,
      interval.getHours,
      interval.getMinutes,
      interval.getSeconds
    )
  }

  def toPgInterval(interval: Interval): PGInterval = {
    new PGInterval(
      interval.years,
      interval.months,
      interval.days,
      interval.hours,
      interval.minutes,
      interval.seconds
    )
  }
}
