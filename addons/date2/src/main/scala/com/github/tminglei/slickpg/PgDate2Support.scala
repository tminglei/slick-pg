package com.github.tminglei.slickpg

import java.time.temporal.ChronoField

import scala.slick.driver.PostgresDriver
import java.time.{Duration, LocalDateTime, LocalTime, LocalDate, ZonedDateTime}
import java.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import org.postgresql.util.PGInterval
import scala.slick.lifted.Column

trait PgDate2Support extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import PgDate2SupportUtils._

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
    val tzDateTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND,0,6,true)
        .optionalEnd()
        .appendOffset("+HH:mm","+00")
        .toFormatter()

    implicit val dateTypeMapper = new GenericJdbcType[LocalDate]("date",
      LocalDate.parse(_, dateFormatter), _.format(dateFormatter), hasLiteralForm=false)
    implicit val timeTypeMapper = new GenericJdbcType[LocalTime]("time",
      LocalTime.parse(_, timeFormatter), _.format(timeFormatter), hasLiteralForm=false)
    implicit val dateTimeTypeMapper = new GenericJdbcType[LocalDateTime]("timestamp",
      LocalDateTime.parse(_, dateTimeFormatter), _.format(dateTimeFormatter), hasLiteralForm=false)
    implicit val durationTypeMapper = new GenericJdbcType[Duration]("interval", pgIntervalStr2Duration, hasLiteralForm=false)
    implicit val timestampTZTypeMapper = new GenericJdbcType[ZonedDateTime]("timestamptz",
      ZonedDateTime.parse(_, tzDateTimeFormatter), _.format(tzDateTimeFormatter), hasLiteralForm=false)

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

    implicit def intervalColumnExtensionMethods(c: Column[Duration]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Duration](c)
    implicit def intervalOptColumnExtensionMethods(c: Column[Option[Duration]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Option[Duration]](c)

    implicit def timestampTZColumnExtensionMethods(c: Column[ZonedDateTime]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, ZonedDateTime, Duration, ZonedDateTime](c)
    implicit def timestampTZOptColumnExtensionMethods(c: Column[Option[ZonedDateTime]]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, ZonedDateTime, Duration, Option[ZonedDateTime]](c)
  }
}

object PgDate2SupportUtils {
  /// pg interval string <-> time.Duration
  def pgIntervalStr2Duration(intervalStr: String): Duration = {
    val pgInterval = new PGInterval(intervalStr)
    Duration.ofDays(pgInterval.getYears * 365 + pgInterval.getMonths * 30 + pgInterval.getDays)
      .plusHours(pgInterval.getHours)
      .plusMinutes(pgInterval.getMinutes)
      .plusNanos(Math.round(pgInterval.getSeconds * 1000 * 1000000))
  }
}
