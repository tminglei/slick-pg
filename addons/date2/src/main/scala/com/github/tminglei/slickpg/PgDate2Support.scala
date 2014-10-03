package com.github.tminglei.slickpg

import java.time.temporal.ChronoField

import scala.slick.driver.PostgresDriver
import java.time._
import java.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import org.postgresql.util.PGInterval
import scala.slick.jdbc.JdbcType
import scala.slick.lifted.Column

trait PgDate2Support extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import PgDate2SupportUtils._

  /// alias
  trait DateTimeImplicits extends Date2DateTimeImplicitsDuration
  trait DateTimeImplicitsPeriod extends Date2DateTimeImplicitsPeriod

  trait Date2DateTimeImplicitsDuration extends BaseDateTimeImplicits[Duration]
  trait Date2DateTimeImplicitsPeriod extends BaseDateTimeImplicits[Period]

  trait BaseDateTimeImplicits[INTERVAL] {
    val date2DateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val date2TimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    val date2DateTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND,0,6,true)
        .optionalEnd()
        .toFormatter()
    val date2TzTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("HH:mm:ss"))
        .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND,0,6,true)
        .optionalEnd()
        .appendOffset("+HH:mm","+00")
        .toFormatter()
    val date2TzDateTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND,0,6,true)
        .optionalEnd()
        .appendOffset("+HH:mm","+00")
        .toFormatter()

    implicit val date2DateTypeMapper = new GenericJdbcType[LocalDate]("date",
      LocalDate.parse(_, date2DateFormatter), _.format(date2DateFormatter), hasLiteralForm=false)
    implicit val date2TimeTypeMapper = new GenericJdbcType[LocalTime]("time",
      LocalTime.parse(_, date2TimeFormatter), _.format(date2TimeFormatter), hasLiteralForm=false)
    implicit val date2DateTimeTypeMapper = new GenericJdbcType[LocalDateTime]("timestamp",
      LocalDateTime.parse(_, date2DateTimeFormatter), _.format(date2DateTimeFormatter), hasLiteralForm=false)
    implicit val date2PeriodTypeMapper = new GenericJdbcType[Period]("interval", pgIntervalStr2Period, hasLiteralForm=false)
    implicit val durationTypeMapper = new GenericJdbcType[Duration]("interval", pgIntervalStr2Duration, hasLiteralForm=false)
    implicit val date2TzTimeTypeMapper = new GenericJdbcType[OffsetTime]("timetz",
      OffsetTime.parse(_, date2TzTimeFormatter), _.format(date2TzTimeFormatter), hasLiteralForm=false)
    implicit val date2TzTimestampTypeMapper = new GenericJdbcType[OffsetDateTime]("timestamptz",
      OffsetDateTime.parse(_, date2TzDateTimeFormatter), _.format(date2TzDateTimeFormatter), hasLiteralForm=false)
    implicit val date2TzTimestamp1TypeMapper = new GenericJdbcType[ZonedDateTime]("timestamptz",
      ZonedDateTime.parse(_, date2TzDateTimeFormatter), _.format(date2TzDateTimeFormatter), hasLiteralForm=false)

    ///
    implicit def date2DateColumnExtensionMethods(c: Column[LocalDate])(implicit tm: JdbcType[INTERVAL]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalDate](c)
    implicit def date2DateOptColumnExtensionMethods(c: Column[Option[LocalDate]])(implicit tm: JdbcType[INTERVAL]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalDate]](c)

    implicit def date2TimeColumnExtensionMethods(c: Column[LocalTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalTime](c)
    implicit def date2TimeOptColumnExtensionMethods(c: Column[Option[LocalTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalTime]](c)

    implicit def date2TimestampColumnExtensionMethods(c: Column[LocalDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalDateTime](c)
    implicit def date2TimestampOptColumnExtensionMethods(c: Column[Option[LocalDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalDateTime]](c)

    implicit def date2IntervalColumnExtensionMethods(c: Column[Period]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Period](c)
    implicit def date2IntervalOptColumnExtensionMethods(c: Column[Option[Period]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[Period]](c)

    implicit def date2Interval1ColumnExtensionMethods(c: Column[Duration]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Duration](c)
    implicit def date2Interval1OptColumnExtensionMethods(c: Column[Option[Duration]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Option[Duration]](c)

    implicit def date2TzTimeColumnExtensionMethods(c: Column[OffsetTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, OffsetTime](c)
    implicit def date2TzTimeOptColumnExtensionMethods(c: Column[Option[OffsetTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, Option[OffsetTime]](c)

    implicit def date2TzTimestampColumnExtensionMethods(c: Column[OffsetDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, OffsetDateTime](c)
    implicit def date2TzTimestampOptColumnExtensionMethods(c: Column[Option[OffsetDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, Option[OffsetDateTime]](c)

    implicit def date2TzTimestamp1ColumnExtensionMethods(c: Column[ZonedDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, INTERVAL, ZonedDateTime](c)
    implicit def date2TzTimestamp1OptColumnExtensionMethods(c: Column[Option[ZonedDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, INTERVAL, Option[ZonedDateTime]](c)

    /// helper classes to INTERVAL column
    implicit class Date2Duration2Period(c: Column[Duration]) {
      def toPeriod: Column[Period] = Column.forNode[Period](c.toNode)
    }
    implicit class Date2DurationOpt2Period(c: Column[Option[Duration]]) {
      def toPeriod: Column[Option[Period]] = Column.forNode[Option[Period]](c.toNode)
    }
    implicit class Date2Period2Duration(c: Column[Period]) {
      def toDuration: Column[Duration] = Column.forNode[Duration](c.toNode)
    }
    implicit class Date2PeriodOpt2Duration(c: Column[Option[Period]]) {
      def toDuration: Column[Option[Duration]] = Column.forNode[Option[Duration]](c.toNode)
    }
  }
}

object PgDate2SupportUtils {
  /// pg interval string --> time.Period
  def pgIntervalStr2Period(intervalStr: String): Period = {
    val pgInterval = new PGInterval(intervalStr)
    Period.of(pgInterval.getYears, pgInterval.getMonths, pgInterval.getDays)
  }

  /// pg interval string --> time.Duration
  def pgIntervalStr2Duration(intervalStr: String): Duration = {
    val pgInterval = new PGInterval(intervalStr)
    Duration.ofDays(pgInterval.getYears * 365 + pgInterval.getMonths * 30 + pgInterval.getDays)
      .plusHours(pgInterval.getHours)
      .plusMinutes(pgInterval.getMinutes)
      .plusNanos(Math.round(pgInterval.getSeconds * 1000 * 1000000))
  }
}
