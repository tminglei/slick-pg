package com.github.tminglei.slickpg

import org.threeten.bp.temporal.ChronoField

import scala.slick.driver.PostgresDriver
import org.threeten.bp._
import org.threeten.bp.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import org.postgresql.util.PGInterval
import scala.slick.jdbc.JdbcType
import scala.slick.lifted.Column

trait PgDateSupport2bp extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import PgThreetenSupportUtils._

  /// alias
  trait DateTimeImplicits extends BpDateTimeImplicitsDuration
  trait DateTimeImplicitsPeriod extends BpDateTimeImplicitsPeriod

  trait BpDateTimeImplicitsDuration extends BaseDateTimeImplicits[Duration]
  trait BpDateTimeImplicitsPeriod extends BaseDateTimeImplicits[Period]

  trait BaseDateTimeImplicits[INTERVAL] {
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
