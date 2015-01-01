package com.github.tminglei.slickpg

import java.time.temporal.ChronoField

import scala.slick.driver.PostgresDriver
import java.time._
import java.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import org.postgresql.util.PGInterval
import scala.slick.jdbc.{PositionedParameters, PositionedResult, JdbcType}
import scala.slick.lifted.Column

trait PgDate2Support extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import PgDate2SupportUtils._

  /// alias
  trait DateTimeImplicits extends Date2DateTimeImplicitsDuration
  trait DateTimeImplicitsPeriod extends Date2DateTimeImplicitsPeriod

  trait Date2DateTimeImplicitsDuration extends Date2DateTimeImplicits[Duration]
  trait Date2DateTimeImplicitsPeriod extends Date2DateTimeImplicits[Period]

  trait Date2DateTimeFormatters {
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
  }

  trait Date2DateTimeImplicits[INTERVAL] extends Date2DateTimeFormatters {
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

  trait Date2DateTimePlainImplicits extends Date2DateTimeFormatters {
    import java.sql.Types

    implicit class PgDate2TimePositionedResult(r: PositionedResult) {
      def nextLocalDate() = nextLocalDateOption().orNull
      def nextLocalDateOption() = r.nextStringOption().map(LocalDate.parse(_, date2DateFormatter))
      def nextLocalTime() = nextLocalTimeOption().orNull
      def nextLocalTimeOption() = r.nextStringOption().map(LocalTime.parse(_, date2TimeFormatter))
      def nextLocalDateTime() = nextLocalDateTimeOption().orNull
      def nextLocalDateTimeOption() = r.nextStringOption().map(LocalDateTime.parse(_, date2DateTimeFormatter))
      def nextOffsetTime() = nextOffsetTimeOption().orNull
      def nextOffsetTimeOption() = r.nextStringOption().map(OffsetTime.parse(_, date2TzTimeFormatter))
      def nextOffsetDateTime() = nextOffsetDateTimeOption().orNull
      def nextOffsetDateTimeOption() = r.nextStringOption().map(OffsetDateTime.parse(_, date2TzDateTimeFormatter))
      def nextZonedDateTime() = nextZonedDateTimeOption().orNull
      def nextZonedDateTimeOption() = r.nextStringOption().map(ZonedDateTime.parse(_, date2TzDateTimeFormatter))
      def nextPeriod() = nextPeriodOption().orNull
      def nextPeriodOption() = r.nextStringOption().map(pgIntervalStr2Period)
      def nextDuration() = nextDurationOption().orNull
      def nextDurationOption() = r.nextStringOption().map(pgIntervalStr2Duration)
    }

    implicit class PgDate2PositionedParameters(p: PositionedParameters) {
      def setLocalDate(v: LocalDate) = setLocalDateOption(Option(v))
      def setLocalDateOption(v: Option[LocalDate]) = setDateTimeInternal(Types.OTHER, "date", v.map(_.format(date2DateFormatter)))
      def setLocalTime(v: LocalTime) = setLocalTimeOption(Option(v))
      def setLocalTimeOption(v: Option[LocalTime]) = setDateTimeInternal(Types.OTHER, "time", v.map(_.format(date2TimeFormatter)))
      def setLocalDateTime(v: LocalDateTime) = setLocalDateTimeOption(Option(v))
      def setLocalDateTimeOption(v: Option[LocalDateTime]) = setDateTimeInternal(Types.OTHER, "timestamp", v.map(_.format(date2DateTimeFormatter)))
      def setOffsetTime(v: OffsetTime) = setOffsetTimeOption(Option(v))
      def setOffsetTimeOption(v: Option[OffsetTime]) = setDateTimeInternal(Types.OTHER, "timetz", v.map(_.format(date2TzTimeFormatter)))
      def setOffsetDateTime(v: OffsetDateTime) = setOffsetDateTimeOption(Option(v))
      def setOffsetDateTimeOption(v: Option[OffsetDateTime]) = setDateTimeInternal(Types.OTHER, "timestamptz", v.map(_.format(date2TzDateTimeFormatter)))
      def setZonedDateTime(v: ZonedDateTime) = setZonedDateTimeOption(Option(v))
      def setZonedDateTimeOption(v: Option[ZonedDateTime]) = setDateTimeInternal(Types.OTHER, "timestamptz", v.map(_.format(date2TzDateTimeFormatter)))
      def setPeriod(v: Period) = setPeriodOption(Option(v))
      def setPeriodOption(v: Option[Period]) = setDateTimeInternal(Types.OTHER, "interval", v.map(_.toString))
      def setDuration(v: Duration) = setDurationOption(Option(v))
      def setDurationOption(v: Option[Duration]) = setDateTimeInternal(Types.OTHER, "interval", v.map(_.toString))
      ///
      private def setDateTimeInternal(sqlType: Int, typeName: String, v: => Option[String]) = {
        p.pos += 1
        v match {
          case Some(v) => p.ps.setObject(p.pos, utils.mkPGobject(typeName, v))
          case None    => p.ps.setNull(p.pos, sqlType)
        }
      }
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
