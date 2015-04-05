package com.github.tminglei.slickpg

import java.time.temporal.ChronoField

import slick.driver.PostgresDriver
import java.time._
import java.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import org.postgresql.util.PGInterval
import slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}

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
  }

  trait Date2DateTimeImplicits[INTERVAL] extends Date2DateTimeFormatters {

    private def fromInfinitable[T](max: T, min: T, parse: String => T): String => T = {
      case "infinity" => max
      case "-infinity" => min
      case finite => parse(finite)
    }
    private val fromDateOrInfinity: String => LocalDate =
      fromInfinitable(LocalDate.MAX, LocalDate.MIN, LocalDate.parse(_, date2DateFormatter))
    private val fromDateTimeOrInfinity: String => LocalDateTime =
      fromInfinitable(LocalDateTime.MAX, LocalDateTime.MIN, LocalDateTime.parse(_, date2DateTimeFormatter))
    private val fromOffsetDateTimeOrInfinity: String => OffsetDateTime = fromInfinitable(
      LocalDateTime.MAX.atOffset(ZoneOffset.UTC),
      LocalDateTime.MIN.atOffset(ZoneOffset.UTC),
      OffsetDateTime.parse(_, date2TzDateTimeFormatter)
    )
    private val fromZonedDateTimeOrInfinity: String => ZonedDateTime = fromInfinitable(
      LocalDateTime.MAX.atZone(ZoneId.of("UTC")),
      LocalDateTime.MIN.atZone(ZoneId.of("UTC")),
      ZonedDateTime.parse(_, date2TzDateTimeFormatter)
    )

    private def toInfinitable[T,U](f: T => U, max: U, min: U, format: T => String): T => String = {
      v => f(v) match {
        case `max` =>  "infinity"
        case `min` =>  "-infinity"
        case _ => format(v)
      }
    }
    private val toDateOrInfinity: LocalDate => String =
      toInfinitable(v => v, LocalDate.MAX, LocalDate.MIN, _.format(date2DateFormatter))
    private val toDateTimeOrInfinity: LocalDateTime => String =
      toInfinitable(v => v, LocalDateTime.MAX, LocalDateTime.MIN, _.format(date2DateTimeFormatter))
    private val toOffsetDateTimeOrInfinity: OffsetDateTime => String =
      toInfinitable(o => o.toLocalDateTime, LocalDateTime.MAX, LocalDateTime.MIN, _.format(date2TzDateTimeFormatter))
    private val toZonedDateTimeOrInfinity: ZonedDateTime => String =
      toInfinitable(z => z.toLocalDateTime, LocalDateTime.MAX, LocalDateTime.MIN, _.format(date2TzDateTimeFormatter))

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
      def nextZoneId() = nextZoneIdOption().orNull
      def nextZoneIdOption() = r.nextStringOption().map(ZoneId.of)
    }

    /////////////////////////////////////////////////////////////////////////////
    implicit object SetLocalDate extends SetParameter[LocalDate] {
      def apply(v: LocalDate, pp: PositionedParameters) = setDateTime(Types.DATE, "date", Option(v).map(_.format(date2DateFormatter)), pp)
    }
    implicit object SetLocalDateOption extends SetParameter[Option[LocalDate]] {
      def apply(v: Option[LocalDate], pp: PositionedParameters) = setDateTime(Types.DATE, "date", v.map(_.format(date2DateFormatter)), pp)
    }
    ///
    implicit object SetLocalTime extends SetParameter[LocalTime] {
      def apply(v: LocalTime, pp: PositionedParameters) = setDateTime(Types.DATE, "time", Option(v).map(_.format(date2TimeFormatter)), pp)
    }
    implicit object SetLocalTimeOption extends SetParameter[Option[LocalTime]] {
      def apply(v: Option[LocalTime], pp: PositionedParameters) = setDateTime(Types.DATE, "time", v.map(_.format(date2TimeFormatter)), pp)
    }
    ///
    implicit object SetLocalDateTime extends SetParameter[LocalDateTime] {
      def apply(v: LocalDateTime, pp: PositionedParameters) = setDateTime(Types.TIMESTAMP, "timestamp", Option(v).map(_.format(date2DateTimeFormatter)), pp)
    }
    implicit object SetLocalDateTimeOption extends SetParameter[Option[LocalDateTime]] {
      def apply(v: Option[LocalDateTime], pp: PositionedParameters) = setDateTime(Types.TIMESTAMP, "timestamp", v.map(_.format(date2DateTimeFormatter)), pp)
    }
    ///
    implicit object SetOffsetTime extends SetParameter[OffsetTime] {
      def apply(v: OffsetTime, pp: PositionedParameters) = setDateTime(Types.TIME_WITH_TIMEZONE, "timetz", Option(v).map(_.format(date2TzTimeFormatter)), pp)
    }
    implicit object SetOffsetTimeOption extends SetParameter[Option[OffsetTime]] {
      def apply(v: Option[OffsetTime], pp: PositionedParameters) = setDateTime(Types.TIME_WITH_TIMEZONE, "timetz", v.map(_.format(date2TzTimeFormatter)), pp)
    }
    ///
    implicit object SetOffsetDateTime extends SetParameter[OffsetDateTime] {
      def apply(v: OffsetDateTime, pp: PositionedParameters) = setDateTime(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", Option(v).map(_.format(date2TzDateTimeFormatter)), pp)
    }
    implicit object SetOffsetDateTimeOption extends SetParameter[Option[OffsetDateTime]] {
      def apply(v: Option[OffsetDateTime], pp: PositionedParameters) = setDateTime(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", v.map(_.format(date2TzDateTimeFormatter)), pp)
    }
    ///
    implicit object SetZonedDateTime extends SetParameter[ZonedDateTime] {
      def apply(v: ZonedDateTime, pp: PositionedParameters) = setDateTime(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", Option(v).map(_.format(date2TzDateTimeFormatter)), pp)
    }
    implicit object SetZonedDateTimeOption extends SetParameter[Option[ZonedDateTime]] {
      def apply(v: Option[ZonedDateTime], pp: PositionedParameters) = setDateTime(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", v.map(_.format(date2TzDateTimeFormatter)), pp)
    }
    ///
    implicit object SetPeriod extends SetParameter[Period] {
      def apply(v: Period, pp: PositionedParameters) = setDateTime(Types.OTHER, "interval", Option(v).map(_.toString), pp)
    }
    implicit object SetPeriodOption extends SetParameter[Option[Period]] {
      def apply(v: Option[Period], pp: PositionedParameters) = setDateTime(Types.OTHER, "interval", v.map(_.toString), pp)
    }
    ///
    implicit object SetDuration extends SetParameter[Duration] {
      def apply(v: Duration, pp: PositionedParameters) = setDateTime(Types.OTHER, "interval", Option(v).map(_.toString), pp)
    }
    implicit object SetDurationOption extends SetParameter[Option[Duration]] {
      def apply(v: Option[Duration], pp: PositionedParameters) = setDateTime(Types.OTHER, "interval", v.map(_.toString), pp)
    }
    ///
    implicit object SetZone extends SetParameter[ZoneId] {
      def apply(v: ZoneId, pp: PositionedParameters) = setDateTime(Types.VARCHAR, "text", Option(v).map(_.toString), pp)
    }
    implicit object SetZoneOption extends SetParameter[Option[ZoneId]] {
      def apply(v: Option[ZoneId], pp: PositionedParameters) = setDateTime(Types.VARCHAR, "text", v.map(_.toString), pp)
    }

    ///
    private def setDateTime(sqlType: Int, typeName: String, v: => Option[String], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject(typeName, v), Types.OTHER)
      case None    => p.setNull(sqlType)
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
