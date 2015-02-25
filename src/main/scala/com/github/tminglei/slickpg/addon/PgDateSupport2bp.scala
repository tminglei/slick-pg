package com.github.tminglei.slickpg

import org.threeten.bp.temporal.ChronoField

import slick.driver.PostgresDriver
import org.threeten.bp._
import org.threeten.bp.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import org.postgresql.util.PGInterval
import slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}

trait PgDateSupport2bp extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import PgThreetenSupportUtils._

  /// alias
  trait DateTimeImplicits extends BpDateTimeImplicitsDuration
  trait DateTimeImplicitsPeriod extends BpDateTimeImplicitsPeriod

  trait BpDateTimeImplicitsDuration extends BpDateTimeImplicits[Duration]
  trait BpDateTimeImplicitsPeriod extends BpDateTimeImplicits[Period]

  trait BpDateTimeFormatters {
    val bpDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val bpTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    val bpDateTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND,0,6,true)
        .optionalEnd()
        .toFormatter()
    val bpTzTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("HH:mm:ss"))
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND,0,6,true)
        .optionalEnd()
        .appendOffset("+HH:mm","+00")
        .toFormatter()
    val bpTzDateTimeFormatter =
      new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND,0,6,true)
        .optionalEnd()
        .appendOffset("+HH:mm","+00")
        .toFormatter()
  }

  trait BpDateTimeImplicits[INTERVAL] extends BpDateTimeFormatters {
    implicit val bpDateTypeMapper = new GenericJdbcType[LocalDate]("date",
      LocalDate.parse(_, bpDateFormatter), _.format(bpDateFormatter), hasLiteralForm=false)
    implicit val bpTimeTypeMapper = new GenericJdbcType[LocalTime]("time",
      LocalTime.parse(_, bpTimeFormatter), _.format(bpTimeFormatter), hasLiteralForm=false)
    implicit val bpDateTimeTypeMapper = new GenericJdbcType[LocalDateTime]("timestamp",
      LocalDateTime.parse(_, bpDateTimeFormatter), _.format(bpDateTimeFormatter), hasLiteralForm=false)
    implicit val bpPeriodTypeMapper = new GenericJdbcType[Period]("interval", pgIntervalStr2Period, hasLiteralForm=false)
    implicit val bpDurationTypeMapper = new GenericJdbcType[Duration]("interval", pgIntervalStr2Duration, hasLiteralForm=false)
    implicit val bpTzTimeTypeMapper = new GenericJdbcType[OffsetTime]("timetz",
      OffsetTime.parse(_, bpTzTimeFormatter), _.format(bpTzTimeFormatter), hasLiteralForm=false)
    implicit val bpTzTimestampTypeMapper = new GenericJdbcType[OffsetDateTime]("timestamptz",
      OffsetDateTime.parse(_, bpTzDateTimeFormatter), _.format(bpTzDateTimeFormatter), hasLiteralForm=false)
    implicit val bpTzTimestamp1TypeMapper = new GenericJdbcType[ZonedDateTime]("timestamptz",
        ZonedDateTime.parse(_, bpTzDateTimeFormatter), _.format(bpTzDateTimeFormatter))

    ///
    implicit def bpDateColumnExtensionMethods(c: Rep[LocalDate])(implicit tm: JdbcType[INTERVAL]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalDate](c)
    implicit def bpDateOptColumnExtensionMethods(c: Rep[Option[LocalDate]])(implicit tm: JdbcType[INTERVAL]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalDate]](c)

    implicit def bpTimeColumnExtensionMethods(c: Rep[LocalTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalTime](c)
    implicit def bpTimeOptColumnExtensionMethods(c: Rep[Option[LocalTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalTime]](c)

    implicit def bpTimestampColumnExtensionMethods(c: Rep[LocalDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalDateTime](c)
    implicit def bpTimestampOptColumnExtensionMethods(c: Rep[Option[LocalDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalDateTime]](c)

    implicit def bpIntervalColumnExtensionMethods(c: Rep[Period]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Period](c)
    implicit def bpIntervalOptColumnExtensionMethods(c: Rep[Option[Period]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[Period]](c)

    implicit def bpInterval1ColumnExtensionMethods(c: Rep[Duration]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Duration](c)
    implicit def bpInterval1OptColumnExtensionMethods(c: Rep[Option[Duration]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Option[Duration]](c)

    implicit def bpTzTimeColumnExtensionMethods(c: Rep[OffsetTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, OffsetTime](c)
    implicit def bpTzTimeOptColumnExtensionMethods(c: Rep[Option[OffsetTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, Option[OffsetTime]](c)

    implicit def bpTzTimestampColumnExtensionMethods(c: Rep[OffsetDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, OffsetDateTime](c)
    implicit def bpTzTimestampOptColumnExtensionMethods(c: Rep[Option[OffsetDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, Option[OffsetDateTime]](c)

    implicit def bpTzTimestamp1ColumnExtensionMethods(c: Rep[ZonedDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, INTERVAL, ZonedDateTime](c)
    implicit def bpTzTimestamp1OptColumnExtensionMethods(c: Rep[Option[ZonedDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, INTERVAL, Option[ZonedDateTime]](c)

    /// helper classes to INTERVAL column
    implicit class BpDuration2Period(c: Rep[Duration]) {
      def toPeriod: Rep[Period] = Rep.forNode[Period](c.toNode)
    }
    implicit class BpDurationOpt2Period(c: Rep[Option[Duration]]) {
      def toPeriod: Rep[Option[Period]] = Rep.forNode[Option[Period]](c.toNode)
    }
    implicit class BpPeriod2Duration(c: Rep[Period]) {
      def toDuration: Rep[Duration] = Rep.forNode[Duration](c.toNode)
    }
    implicit class BpPeriodOpt2Duration(c: Rep[Option[Period]]) {
      def toDuration: Rep[Option[Duration]] = Rep.forNode[Option[Duration]](c.toNode)
    }
  }

  trait BpDateTimePlainImplicits extends BpDateTimeFormatters {
    import java.sql.Types

    implicit class PgDate2TimePositionedResult(r: PositionedResult) {
      def nextLocalDate() = nextLocalDateOption().orNull
      def nextLocalDateOption() = r.nextStringOption().map(LocalDate.parse(_, bpDateFormatter))
      def nextLocalTime() = nextLocalTimeOption().orNull
      def nextLocalTimeOption() = r.nextStringOption().map(LocalTime.parse(_, bpTimeFormatter))
      def nextLocalDateTime() = nextLocalDateTimeOption().orNull
      def nextLocalDateTimeOption() = r.nextStringOption().map(LocalDateTime.parse(_, bpDateTimeFormatter))
      def nextOffsetTime() = nextOffsetTimeOption().orNull
      def nextOffsetTimeOption() = r.nextStringOption().map(OffsetTime.parse(_, bpTzTimeFormatter))
      def nextOffsetDateTime() = nextOffsetDateTimeOption().orNull
      def nextOffsetDateTimeOption() = r.nextStringOption().map(OffsetDateTime.parse(_, bpTzDateTimeFormatter))
      def nextZonedDateTime() = nextZonedDateTimeOption().orNull
      def nextZonedDateTimeOption() = r.nextStringOption().map(ZonedDateTime.parse(_, bpTzDateTimeFormatter))
      def nextPeriod() = nextPeriodOption().orNull
      def nextPeriodOption() = r.nextStringOption().map(pgIntervalStr2Period)
      def nextDuration() = nextDurationOption().orNull
      def nextDurationOption() = r.nextStringOption().map(pgIntervalStr2Duration)
    }

    /////////////////////////////////////////////////////////////////////////////
    implicit object SetLocalDate extends SetParameter[LocalDate] {
      def apply(v: LocalDate, pp: PositionedParameters) = setDateTime(Types.DATE, "date", Option(v).map(_.format(bpDateFormatter)), pp)
    }
    implicit object SetLocalDateOption extends SetParameter[Option[LocalDate]] {
      def apply(v: Option[LocalDate], pp: PositionedParameters) = setDateTime(Types.DATE, "date", v.map(_.format(bpDateFormatter)), pp)
    }
    ///
    implicit object SetLocalTime extends SetParameter[LocalTime] {
      def apply(v: LocalTime, pp: PositionedParameters) = setDateTime(Types.DATE, "time", Option(v).map(_.format(bpTimeFormatter)), pp)
    }
    implicit object SetLocalTimeOption extends SetParameter[Option[LocalTime]] {
      def apply(v: Option[LocalTime], pp: PositionedParameters) = setDateTime(Types.DATE, "time", v.map(_.format(bpTimeFormatter)), pp)
    }
    ///
    implicit object SetLocalDateTime extends SetParameter[LocalDateTime] {
      def apply(v: LocalDateTime, pp: PositionedParameters) = setDateTime(Types.TIMESTAMP, "timestamp", Option(v).map(_.format(bpDateTimeFormatter)), pp)
    }
    implicit object SetLocalDateTimeOption extends SetParameter[Option[LocalDateTime]] {
      def apply(v: Option[LocalDateTime], pp: PositionedParameters) = setDateTime(Types.TIMESTAMP, "timestamp", v.map(_.format(bpDateTimeFormatter)), pp)
    }
    ///
    implicit object SetOffsetTime extends SetParameter[OffsetTime] {
      def apply(v: OffsetTime, pp: PositionedParameters) = setDateTime(Types.TIME_WITH_TIMEZONE, "timetz", Option(v).map(_.format(bpTzTimeFormatter)), pp)
    }
    implicit object SetOffsetTimeOption extends SetParameter[Option[OffsetTime]] {
      def apply(v: Option[OffsetTime], pp: PositionedParameters) = setDateTime(Types.TIME_WITH_TIMEZONE, "timetz", v.map(_.format(bpTzTimeFormatter)), pp)
    }
    ///
    implicit object SetOffsetDateTime extends SetParameter[OffsetDateTime] {
      def apply(v: OffsetDateTime, pp: PositionedParameters) = setDateTime(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", Option(v).map(_.format(bpTzDateTimeFormatter)), pp)
    }
    implicit object SetOffsetDateTimeOption extends SetParameter[Option[OffsetDateTime]] {
      def apply(v: Option[OffsetDateTime], pp: PositionedParameters) = setDateTime(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", v.map(_.format(bpTzDateTimeFormatter)), pp)
    }
    ///
    implicit object SetZonedDateTime extends SetParameter[ZonedDateTime] {
      def apply(v: ZonedDateTime, pp: PositionedParameters) = setDateTime(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", Option(v).map(_.format(bpTzDateTimeFormatter)), pp)
    }
    implicit object SetZonedDateTimeOption extends SetParameter[Option[ZonedDateTime]] {
      def apply(v: Option[ZonedDateTime], pp: PositionedParameters) = setDateTime(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", v.map(_.format(bpTzDateTimeFormatter)), pp)
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
    private def setDateTime(sqlType: Int, typeName: String, v: => Option[String], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject(typeName, v), Types.OTHER)
      case None    => p.setNull(sqlType)
    }
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
