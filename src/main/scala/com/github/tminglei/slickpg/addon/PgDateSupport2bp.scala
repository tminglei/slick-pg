package com.github.tminglei.slickpg

import org.threeten.bp.temporal.ChronoField

import scala.slick.driver.PostgresDriver
import org.threeten.bp._
import org.threeten.bp.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import org.postgresql.util.PGInterval
import scala.slick.jdbc.{PositionedParameters, PositionedResult, JdbcType}
import scala.slick.lifted.Column

trait PgDateSupport2bp extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
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
    implicit def bpDateColumnExtensionMethods(c: Column[LocalDate])(implicit tm: JdbcType[INTERVAL]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalDate](c)
    implicit def bpDateOptColumnExtensionMethods(c: Column[Option[LocalDate]])(implicit tm: JdbcType[INTERVAL]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalDate]](c)

    implicit def bpTimeColumnExtensionMethods(c: Column[LocalTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalTime](c)
    implicit def bpTimeOptColumnExtensionMethods(c: Column[Option[LocalTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalTime]](c)

    implicit def bpTimestampColumnExtensionMethods(c: Column[LocalDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalDateTime](c)
    implicit def bpTimestampOptColumnExtensionMethods(c: Column[Option[LocalDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalDateTime]](c)

    implicit def bpIntervalColumnExtensionMethods(c: Column[Period]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Period](c)
    implicit def bpIntervalOptColumnExtensionMethods(c: Column[Option[Period]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[Period]](c)

    implicit def bpInterval1ColumnExtensionMethods(c: Column[Duration]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Duration](c)
    implicit def bpInterval1OptColumnExtensionMethods(c: Column[Option[Duration]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Option[Duration]](c)

    implicit def bpTzTimeColumnExtensionMethods(c: Column[OffsetTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, OffsetTime](c)
    implicit def bpTzTimeOptColumnExtensionMethods(c: Column[Option[OffsetTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, Option[OffsetTime]](c)

    implicit def bpTzTimestampColumnExtensionMethods(c: Column[OffsetDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, OffsetDateTime](c)
    implicit def bpTzTimestampOptColumnExtensionMethods(c: Column[Option[OffsetDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, INTERVAL, Option[OffsetDateTime]](c)

    implicit def bpTzTimestamp1ColumnExtensionMethods(c: Column[ZonedDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, INTERVAL, ZonedDateTime](c)
    implicit def bpTzTimestamp1OptColumnExtensionMethods(c: Column[Option[ZonedDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, INTERVAL, Option[ZonedDateTime]](c)

    /// helper classes to INTERVAL column
    implicit class BpDuration2Period(c: Column[Duration]) {
      def toPeriod: Column[Period] = Column.forNode[Period](c.toNode)
    }
    implicit class BpDurationOpt2Period(c: Column[Option[Duration]]) {
      def toPeriod: Column[Option[Period]] = Column.forNode[Option[Period]](c.toNode)
    }
    implicit class BpPeriod2Duration(c: Column[Period]) {
      def toDuration: Column[Duration] = Column.forNode[Duration](c.toNode)
    }
    implicit class BpPeriodOpt2Duration(c: Column[Option[Period]]) {
      def toDuration: Column[Option[Duration]] = Column.forNode[Option[Duration]](c.toNode)
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

    implicit class PgDate2PositionedParameters(p: PositionedParameters) {
      def setLocalDate(v: LocalDate) = setLocalDateOption(Option(v))
      def setLocalDateOption(v: Option[LocalDate]) = setDateTimeInternal(Types.OTHER, "date", v.map(_.format(bpDateFormatter)))
      def setLocalTime(v: LocalTime) = setLocalTimeOption(Option(v))
      def setLocalTimeOption(v: Option[LocalTime]) = setDateTimeInternal(Types.OTHER, "time", v.map(_.format(bpTimeFormatter)))
      def setLocalDateTime(v: LocalDateTime) = setLocalDateTimeOption(Option(v))
      def setLocalDateTimeOption(v: Option[LocalDateTime]) = setDateTimeInternal(Types.OTHER, "timestamp", v.map(_.format(bpDateTimeFormatter)))
      def setOffsetTime(v: OffsetTime) = setOffsetTimeOption(Option(v))
      def setOffsetTimeOption(v: Option[OffsetTime]) = setDateTimeInternal(Types.OTHER, "timetz", v.map(_.format(bpTzTimeFormatter)))
      def setOffsetDateTime(v: OffsetDateTime) = setOffsetDateTimeOption(Option(v))
      def setOffsetDateTimeOption(v: Option[OffsetDateTime]) = setDateTimeInternal(Types.OTHER, "timestamptz", v.map(_.format(bpTzDateTimeFormatter)))
      def setZonedDateTime(v: ZonedDateTime) = setZonedDateTimeOption(Option(v))
      def setZonedDateTimeOption(v: Option[ZonedDateTime]) = setDateTimeInternal(Types.OTHER, "timestamptz", v.map(_.format(bpTzDateTimeFormatter)))
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
