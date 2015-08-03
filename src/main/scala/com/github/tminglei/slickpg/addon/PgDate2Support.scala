package com.github.tminglei.slickpg

import java.time.temporal.ChronoField

import slick.driver.PostgresDriver
import java.time._
import java.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import org.postgresql.util.PGInterval
import slick.jdbc.{PositionedResult, JdbcType}

trait PgDate2Support extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
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

    protected def fromInfinitable[T](max: T, min: T, parse: String => T): String => T = {
      case "infinity" => max
      case "-infinity" => min
      case finite => parse(finite)
    }
    protected val fromDateOrInfinity: String => LocalDate =
      fromInfinitable(LocalDate.MAX, LocalDate.MIN, LocalDate.parse(_, date2DateFormatter))
    protected val fromDateTimeOrInfinity: String => LocalDateTime =
      fromInfinitable(LocalDateTime.MAX, LocalDateTime.MIN, LocalDateTime.parse(_, date2DateTimeFormatter))
    protected val fromOffsetDateTimeOrInfinity: String => OffsetDateTime =
      fromInfinitable(OffsetDateTime.MAX, OffsetDateTime.MIN, OffsetDateTime.parse(_, date2TzDateTimeFormatter))
    protected val fromZonedDateTimeOrInfinity: String => ZonedDateTime = fromInfinitable(
      LocalDateTime.MAX.atZone(ZoneId.of("UTC")), LocalDateTime.MIN.atZone(ZoneId.of("UTC")),
      ZonedDateTime.parse(_, date2TzDateTimeFormatter)
    )
    ///
    protected def toInfinitable[T](max: T, min: T, format: T => String): T => String = {
      case `max` =>  "infinity"
      case `min` =>  "-infinity"
      case finite => format(finite)
    }
    protected val toDateOrInfinity: LocalDate => String =
      toInfinitable[LocalDate](LocalDate.MAX, LocalDate.MIN, _.format(date2DateFormatter))
    protected val toDateTimeOrInfinity: LocalDateTime => String =
      toInfinitable[LocalDateTime](LocalDateTime.MAX, LocalDateTime.MIN, _.format(date2DateTimeFormatter))
    protected val toOffsetDateTimeOrInfinity: OffsetDateTime => String =
      toInfinitable[OffsetDateTime](OffsetDateTime.MAX, OffsetDateTime.MIN, _.format(date2TzDateTimeFormatter))
    protected val toZonedDateTimeOrInfinity: ZonedDateTime => String =
      toInfinitable[ZonedDateTime](LocalDateTime.MAX.atZone(ZoneId.of("UTC")), LocalDateTime.MIN.atZone(ZoneId.of("UTC")),
      _.format(date2TzDateTimeFormatter)
    )
  }

  trait Date2DateTimeImplicits[INTERVAL] extends Date2DateTimeFormatters {
    implicit val date2DateTypeMapper = new GenericJdbcType[LocalDate]("date",
      fromDateOrInfinity, toDateOrInfinity, hasLiteralForm=false)
    implicit val date2TimeTypeMapper = new GenericJdbcType[LocalTime]("time",
      LocalTime.parse(_, date2TimeFormatter), _.format(date2TimeFormatter), hasLiteralForm=false)
    implicit val date2DateTimeTypeMapper = new GenericJdbcType[LocalDateTime]("timestamp",
      fromDateTimeOrInfinity, toDateTimeOrInfinity, hasLiteralForm=false)
    implicit val date2PeriodTypeMapper = new GenericJdbcType[Period]("interval", pgIntervalStr2Period, hasLiteralForm=false)
    implicit val durationTypeMapper = new GenericJdbcType[Duration]("interval", pgIntervalStr2Duration, hasLiteralForm=false)
    implicit val date2TzTimeTypeMapper = new GenericJdbcType[OffsetTime]("timetz",
      OffsetTime.parse(_, date2TzTimeFormatter), _.format(date2TzTimeFormatter), hasLiteralForm=false)
    implicit val date2TzTimestampTypeMapper = new GenericJdbcType[OffsetDateTime]("timestamptz",
      fromOffsetDateTimeOrInfinity, toOffsetDateTimeOrInfinity, hasLiteralForm=false)
    implicit val date2TzTimestamp1TypeMapper = new GenericJdbcType[ZonedDateTime]("timestamptz",
      fromZonedDateTimeOrInfinity, toZonedDateTimeOrInfinity, hasLiteralForm=false)
    implicit val date2ZoneIdMapper = new GenericJdbcType[ZoneId]("text", ZoneId.of(_), _.getId, hasLiteralForm=false)

    ///
    implicit def date2DateColumnExtensionMethods(c: Rep[LocalDate])(implicit tm: JdbcType[INTERVAL]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalDate](c)
    implicit def date2DateOptColumnExtensionMethods(c: Rep[Option[LocalDate]])(implicit tm: JdbcType[INTERVAL]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalDate]](c)

    implicit def date2TimeColumnExtensionMethods(c: Rep[LocalTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalTime](c)
    implicit def date2TimeOptColumnExtensionMethods(c: Rep[Option[LocalTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalTime]](c)

    implicit def date2TimestampColumnExtensionMethods(c: Rep[LocalDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalDateTime](c)
    implicit def date2TimestampOptColumnExtensionMethods(c: Rep[Option[LocalDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalDateTime]](c)

    implicit def date2IntervalColumnExtensionMethods(c: Rep[Period]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Period](c)
    implicit def date2IntervalOptColumnExtensionMethods(c: Rep[Option[Period]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[Period]](c)

    implicit def date2Interval1ColumnExtensionMethods(c: Rep[Duration]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Duration](c)
    implicit def date2Interval1OptColumnExtensionMethods(c: Rep[Option[Duration]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Option[Duration]](c)

    implicit def date2TzTimeColumnExtensionMethods(c: Rep[OffsetTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, OffsetTime](c)
    implicit def date2TzTimeOptColumnExtensionMethods(c: Rep[Option[OffsetTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, Option[OffsetTime]](c)

    implicit def date2TzTimestampColumnExtensionMethods(c: Rep[OffsetDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, OffsetDateTime](c)
    implicit def date2TzTimestampOptColumnExtensionMethods(c: Rep[Option[OffsetDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, Option[OffsetDateTime]](c)

    implicit def date2TzTimestamp1ColumnExtensionMethods(c: Rep[ZonedDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, INTERVAL, ZonedDateTime](c)
    implicit def date2TzTimestamp1OptColumnExtensionMethods(c: Rep[Option[ZonedDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, INTERVAL, Option[ZonedDateTime]](c)

    /// helper classes to INTERVAL column
    implicit class Date2Duration2Period(c: Rep[Duration]) {
      def toPeriod: Rep[Period] = Rep.forNode[Period](c.toNode)
    }
    implicit class Date2DurationOpt2Period(c: Rep[Option[Duration]]) {
      def toPeriod: Rep[Option[Period]] = Rep.forNode[Option[Period]](c.toNode)
    }
    implicit class Date2Period2Duration(c: Rep[Period]) {
      def toDuration: Rep[Duration] = Rep.forNode[Duration](c.toNode)
    }
    implicit class Date2PeriodOpt2Duration(c: Rep[Option[Period]]) {
      def toDuration: Rep[Option[Duration]] = Rep.forNode[Option[Duration]](c.toNode)
    }
  }

  trait Date2DateTimePlainImplicits extends Date2DateTimeFormatters {
    import java.sql.Types
    import utils.PlainSQLUtils._

    implicit class PgDate2TimePositionedResult(r: PositionedResult) {
      def nextLocalDate() = nextLocalDateOption().orNull
      def nextLocalDateOption() = r.nextStringOption().map(fromDateOrInfinity)
      def nextLocalTime() = nextLocalTimeOption().orNull
      def nextLocalTimeOption() = r.nextStringOption().map(LocalTime.parse(_, date2TimeFormatter))
      def nextLocalDateTime() = nextLocalDateTimeOption().orNull
      def nextLocalDateTimeOption() = r.nextStringOption().map(fromDateTimeOrInfinity)
      def nextOffsetTime() = nextOffsetTimeOption().orNull
      def nextOffsetTimeOption() = r.nextStringOption().map(OffsetTime.parse(_, date2TzTimeFormatter))
      def nextOffsetDateTime() = nextOffsetDateTimeOption().orNull
      def nextOffsetDateTimeOption() = r.nextStringOption().map(fromOffsetDateTimeOrInfinity)
      def nextZonedDateTime() = nextZonedDateTimeOption().orNull
      def nextZonedDateTimeOption() = r.nextStringOption().map(fromZonedDateTimeOrInfinity)
      def nextPeriod() = nextPeriodOption().orNull
      def nextPeriodOption() = r.nextStringOption().map(pgIntervalStr2Period)
      def nextDuration() = nextDurationOption().orNull
      def nextDurationOption() = r.nextStringOption().map(pgIntervalStr2Duration)
      def nextZoneId() = nextZoneIdOption().orNull
      def nextZoneIdOption() = r.nextStringOption().map(ZoneId.of)
    }

    /////////////////////////////////////////////////////////////////////////////
    implicit val getLocalDate = mkGetResult(_.nextLocalDate())
    implicit val getLocalDateOption = mkGetResult(_.nextLocalDateOption())
    implicit val setLocalDate = mkSetParameter[LocalDate]("date", toDateOrInfinity, sqlType = Types.DATE)
    implicit val setLocalDateOption = mkOptionSetParameter[LocalDate]("date", toDateOrInfinity, sqlType = Types.DATE)

    implicit val getLocalTime = mkGetResult(_.nextLocalTime())
    implicit val getLocalTimeOption = mkGetResult(_.nextLocalTimeOption())
    implicit val setLocalTime = mkSetParameter[LocalTime]("time", _.format(date2TimeFormatter), sqlType = Types.TIME)
    implicit val setLocalTimeOption = mkOptionSetParameter[LocalTime]("time", _.format(date2TimeFormatter), sqlType = Types.TIME)

    implicit val getLocalDateTime = mkGetResult(_.nextLocalDateTime())
    implicit val getLocalDateTimeOption = mkGetResult(_.nextLocalDateTimeOption())
    implicit val setLocalDateTime = mkSetParameter[LocalDateTime]("timestamp", toDateTimeOrInfinity, sqlType = Types.TIMESTAMP)
    implicit val setLocalDateTimeOption = mkOptionSetParameter[LocalDateTime]("timestamp", toDateTimeOrInfinity, sqlType = Types.TIMESTAMP)

    implicit val getOffsetTime = mkGetResult(_.nextOffsetTime())
    implicit val getOffsetTimeOption = mkGetResult(_.nextOffsetTimeOption())
    implicit val setOffsetTime = mkSetParameter[OffsetTime]("timetz", _.format(date2TzTimeFormatter), sqlType = Types.TIME_WITH_TIMEZONE)
    implicit val setOffsetTimeOption = mkOptionSetParameter[OffsetTime]("timetz", _.format(date2TzTimeFormatter), sqlType = Types.TIME_WITH_TIMEZONE)

    implicit val getOffsetDateTime = mkGetResult(_.nextOffsetDateTime())
    implicit val getOffsetDateTimeOption = mkGetResult(_.nextOffsetDateTimeOption())
    implicit val setOffsetDateTime = mkSetParameter[OffsetDateTime]("timestamptz", toOffsetDateTimeOrInfinity, sqlType = Types.TIMESTAMP_WITH_TIMEZONE)
    implicit val setOffsetDateTimeOption = mkOptionSetParameter[OffsetDateTime]("timestamptz", toOffsetDateTimeOrInfinity, sqlType = Types.TIMESTAMP_WITH_TIMEZONE)

    implicit val getZonedDateTime = mkGetResult(_.nextZonedDateTime())
    implicit val getZonedDateTimeOption = mkGetResult(_.nextZonedDateTimeOption())
    implicit val setZonedDateTime = mkSetParameter[ZonedDateTime]("timestamptz", toZonedDateTimeOrInfinity, sqlType = Types.TIMESTAMP_WITH_TIMEZONE)
    implicit val setZonedDateTimeOption = mkOptionSetParameter[ZonedDateTime]("timestamptz", toZonedDateTimeOrInfinity, sqlType = Types.TIMESTAMP_WITH_TIMEZONE)

    implicit val getPeriod = mkGetResult(_.nextPeriod())
    implicit val getPeriodOption = mkGetResult(_.nextPeriodOption())
    implicit val setPeriod = mkSetParameter[Period]("interval")
    implicit val setPeriodOption = mkOptionSetParameter[Period]("interval")

    implicit val getDuration = mkGetResult(_.nextDuration())
    implicit val getDurationOption = mkGetResult(_.nextDurationOption())
    implicit val setDuration = mkSetParameter[Duration]("interval")
    implicit val setDurationOption = mkOptionSetParameter[Duration]("interval")

    implicit val getZoneId = mkGetResult(_.nextZoneId())
    implicit val getZoneIdOption = mkGetResult(_.nextZoneIdOption())
    implicit val setZoneId = mkSetParameter[ZoneId]("text", sqlType = Types.VARCHAR)
    implicit val setZoneIdOption = mkOptionSetParameter[ZoneId]("text", sqlType = Types.VARCHAR)
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
