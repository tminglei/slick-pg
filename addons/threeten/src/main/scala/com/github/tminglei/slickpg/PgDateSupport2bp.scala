package com.github.tminglei.slickpg

import org.threeten.bp.temporal.ChronoField

import scala.slick.driver.PostgresDriver
import org.threeten.bp._
import org.threeten.bp.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import org.postgresql.util.PGInterval
import scala.slick.lifted.Column

trait PgDateSupport2bp extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import PgThreetenSupportUtils._

  trait DateTimeImplicits {
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    val dateTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND,0,6,true)
        .optionalEnd()
        .toFormatter()
    val tzTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("HH:mm:ss"))
        .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND,0,6,true)
        .optionalEnd()
        .appendOffset("+HH:mm","+00")
        .toFormatter()
    val tzDateTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND,0,6,true)
        .optionalEnd()
        .appendOffset("+HH:mm","+00")
        .toFormatter()

    implicit val bpDateTypeMapper = new GenericJdbcType[LocalDate]("date",
      LocalDate.parse(_, dateFormatter), _.format(dateFormatter), hasLiteralForm=false)
    implicit val bpTimeTypeMapper = new GenericJdbcType[LocalTime]("time",
      LocalTime.parse(_, timeFormatter), _.format(timeFormatter), hasLiteralForm=false)
    implicit val bpDateTimeTypeMapper = new GenericJdbcType[LocalDateTime]("timestamp",
      LocalDateTime.parse(_, dateTimeFormatter), _.format(dateTimeFormatter), hasLiteralForm=false)
    implicit val bpPeriodTypeMapper = new GenericJdbcType[Period]("interval", pgIntervalStr2Period, hasLiteralForm=false)
    implicit val bpDurationTypeMapper = new GenericJdbcType[Duration]("interval", pgIntervalStr2Duration, hasLiteralForm=false)
    implicit val tzTimeTypeMapper = new GenericJdbcType[OffsetTime]("timetz",
      OffsetTime.parse(_, tzTimeFormatter), _.format(tzTimeFormatter), hasLiteralForm=false)
    implicit val tzTimestampTypeMapper = new GenericJdbcType[OffsetDateTime]("timestamptz",
      OffsetDateTime.parse(_, tzDateTimeFormatter), _.format(tzDateTimeFormatter), hasLiteralForm=false)
    implicit val tzTimestamp1TypeMapper = new GenericJdbcType[ZonedDateTime]("timestamptz",
        ZonedDateTime.parse(_, tzDateTimeFormatter), _.format(tzDateTimeFormatter))

    ///
    implicit def dateColumnExtensionMethods(c: Column[LocalDate]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, LocalDate](c)
    implicit def dateOptColumnExtensionMethods(c: Column[Option[LocalDate]]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Option[LocalDate]](c)

    implicit def timeColumnExtensionMethods(c: Column[LocalTime]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, LocalTime](c)
    implicit def timeOptColumnExtensionMethods(c: Column[Option[LocalTime]]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Option[LocalTime]](c)

    implicit def timestampColumnExtensionMethods(c: Column[LocalDateTime]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, LocalDateTime](c)
    implicit def timestampOptColumnExtensionMethods(c: Column[Option[LocalDateTime]]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Option[LocalDateTime]](c)

    implicit def intervalColumnExtensionMethods(c: Column[Period]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Period](c)
    implicit def intervalOptColumnExtensionMethods(c: Column[Option[Period]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[Period]](c)

    implicit def interval1ColumnExtensionMethods(c: Column[Duration]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Duration](c)
    implicit def interval1OptColumnExtensionMethods(c: Column[Option[Duration]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Option[Duration]](c)

    implicit def tzTimeColumnExtensionMethods(c: Column[OffsetTime]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, Duration, OffsetTime](c)
    implicit def tzTimeOptColumnExtensionMethods(c: Column[Option[OffsetTime]]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, Duration, Option[OffsetTime]](c)

    implicit def tzTimestampColumnExtensionMethods(c: Column[OffsetDateTime]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, Duration, OffsetDateTime](c)
    implicit def tzTimestampOptColumnExtensionMethods(c: Column[Option[OffsetDateTime]]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, Duration, Option[OffsetDateTime]](c)

    implicit def tzTimestamp1ColumnExtensionMethods(c: Column[ZonedDateTime]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, Duration, ZonedDateTime](c)
    implicit def tzTimestamp1OptColumnExtensionMethods(c: Column[Option[ZonedDateTime]]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, Duration, Option[ZonedDateTime]](c)
  }
}

object PgThreetenSupportUtils {
  /// pg interval string --> time.Period
  def pgIntervalStr2Period(intervalStr: String): Period = {
    val pgInterval = new PGInterval(intervalStr)
    Period.of(pgInterval.getYears, pgInterval.getMonths, pgInterval.getDays)
  }

  /// pg interval string --> bp.Duration
  def pgIntervalStr2Duration(intervalStr: String): Duration = {
    val pgInterval = new PGInterval(intervalStr)
    Duration.ofDays(pgInterval.getYears * 365 + pgInterval.getMonths * 30 + pgInterval.getDays)
      .plusHours(pgInterval.getHours)
      .plusMinutes(pgInterval.getMinutes)
      .plusNanos(Math.round(pgInterval.getSeconds * 1000 * 1000000))
  }
}
