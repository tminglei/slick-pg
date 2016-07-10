package com.github.tminglei.slickpg

import org.postgresql.util.PGInterval
import org.threeten.bp._
import org.threeten.bp.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import org.threeten.bp.temporal.ChronoField
import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}

import scala.reflect.{ClassTag, classTag}

trait PgDateSupport2bp extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import PgThreetenSupportUtils._
  import driver.api._

  // let user to call this, since we have more than one `TIMETZ, DATETIMETZ, INTERVAL` binding candidates here
  def bindPgDateTypesToScala[DATE, TIME, DATETIME, TIMETZ, DATETIMETZ, INTERVAL](
          implicit ctag1: ClassTag[DATE], ctag2: ClassTag[TIME], ctag3: ClassTag[DATETIME],
                   ctag4: ClassTag[TIMETZ], ctag5: ClassTag[DATETIMETZ], ctag6: ClassTag[INTERVAL]) = {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("date", classTag[DATE])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("time", classTag[TIME])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("timestamp", classTag[DATETIME])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("timetz", classTag[TIMETZ])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("timestamptz", classTag[DATETIMETZ])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("interval", classTag[INTERVAL])
    }
    else throw new IllegalArgumentException("The driver MUST BE a `ExPostgresProfile`!")
  }

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
    implicit val bpDateTypeMapper: JdbcType[LocalDate] = new GenericJdbcType[LocalDate]("date",
      LocalDate.parse(_, bpDateFormatter), _.format(bpDateFormatter), hasLiteralForm=false)
    implicit val bpTimeTypeMapper: JdbcType[LocalTime] = new GenericJdbcType[LocalTime]("time",
      LocalTime.parse(_, bpTimeFormatter), _.format(bpTimeFormatter), hasLiteralForm=false)
    implicit val bpDateTimeTypeMapper: JdbcType[LocalDateTime] = new GenericJdbcType[LocalDateTime]("timestamp",
      LocalDateTime.parse(_, bpDateTimeFormatter), _.format(bpDateTimeFormatter), hasLiteralForm=false)
    implicit val bpPeriodTypeMapper: JdbcType[Period] = new GenericJdbcType[Period]("interval", pgIntervalStr2Period, hasLiteralForm=false)
    implicit val bpDurationTypeMapper: JdbcType[Duration] = new GenericJdbcType[Duration]("interval", pgIntervalStr2Duration, hasLiteralForm=false)
    implicit val bpTzTimeTypeMapper: JdbcType[OffsetTime] = new GenericJdbcType[OffsetTime]("timetz",
      OffsetTime.parse(_, bpTzTimeFormatter), _.format(bpTzTimeFormatter), hasLiteralForm=false)
    implicit val bpTzTimestampTypeMapper: JdbcType[OffsetDateTime] = new GenericJdbcType[OffsetDateTime]("timestamptz",
      OffsetDateTime.parse(_, bpTzDateTimeFormatter), _.format(bpTzDateTimeFormatter), hasLiteralForm=false)
    implicit val bpTzTimestamp1TypeMapper: JdbcType[ZonedDateTime] = new GenericJdbcType[ZonedDateTime]("timestamptz",
        ZonedDateTime.parse(_, bpTzDateTimeFormatter), _.format(bpTzDateTimeFormatter))

    ///
    implicit def bpDateColumnExtensionMethods(c: Rep[LocalDate])(implicit tm: JdbcType[INTERVAL]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, LocalDate](c)
    implicit def bpDateOptColumnExtensionMethods(c: Rep[Option[LocalDate]])(implicit tm: JdbcType[INTERVAL]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, INTERVAL, Option[LocalDate]](c)

    implicit def bpTimeColumnExtensionMethods(c: Rep[LocalTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, OffsetTime, INTERVAL, LocalTime](c)
    implicit def bpTimeOptColumnExtensionMethods(c: Rep[Option[LocalTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, OffsetTime, INTERVAL, Option[LocalTime]](c)

    implicit def bpTimestampColumnExtensionMethods(c: Rep[LocalDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, OffsetDateTime, INTERVAL, LocalDateTime](c)
    implicit def bpTimestampOptColumnExtensionMethods(c: Rep[Option[LocalDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, OffsetDateTime, INTERVAL, Option[LocalDateTime]](c)

    implicit def bpIntervalColumnExtensionMethods(c: Rep[Period]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Period](c)
    implicit def bpIntervalOptColumnExtensionMethods(c: Rep[Option[Period]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[Period]](c)

    implicit def bpInterval1ColumnExtensionMethods(c: Rep[Duration]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Duration](c)
    implicit def bpInterval1OptColumnExtensionMethods(c: Rep[Option[Duration]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Duration, Option[Duration]](c)

    implicit def bpTzTimeColumnExtensionMethods(c: Rep[OffsetTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, LocalDateTime, INTERVAL, OffsetTime](c)
    implicit def bpTzTimeOptColumnExtensionMethods(c: Rep[Option[OffsetTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, LocalDateTime, INTERVAL, Option[OffsetTime]](c)

    implicit def bpTzTimestampColumnExtensionMethods(c: Rep[OffsetDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, LocalDateTime, INTERVAL, OffsetDateTime](c)
    implicit def bpTzTimestampOptColumnExtensionMethods(c: Rep[Option[OffsetDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, OffsetDateTime, LocalDateTime, INTERVAL, Option[OffsetDateTime]](c)

    implicit def bpTzTimestamp1ColumnExtensionMethods(c: Rep[ZonedDateTime])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, LocalDateTime, INTERVAL, ZonedDateTime](c)
    implicit def bpTzTimestamp1OptColumnExtensionMethods(c: Rep[Option[ZonedDateTime]])(implicit tm: JdbcType[INTERVAL]) =
      new TimestampColumnExtensionMethods[LocalDate, OffsetTime, ZonedDateTime, LocalDateTime, INTERVAL, Option[ZonedDateTime]](c)

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

    import utils.PlainSQLUtils._

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
      def nextZoneId() = nextZoneIdOption().orNull
      def nextZoneIdOption() = r.nextStringOption().map(ZoneId.of)
    }

    /////////////////////////////////////////////////////////////////////////////
    implicit val getLocalDate = mkGetResult(_.nextLocalDate())
    implicit val getLocalDateOption = mkGetResult(_.nextLocalDateOption())
    implicit val setLocalDate = mkSetParameter[LocalDate]("date", _.format(bpDateFormatter), sqlType = Types.DATE)
    implicit val setLocalDateOption = mkOptionSetParameter[LocalDate]("date", _.format(bpDateFormatter), sqlType = Types.DATE)

    implicit val getLocalTime = mkGetResult(_.nextLocalTime())
    implicit val getLocalTimeOption = mkGetResult(_.nextLocalTimeOption())
    implicit val setLocalTime = mkSetParameter[LocalTime]("time", _.format(bpTimeFormatter), sqlType = Types.TIME)
    implicit val setLocalTimeOption = mkOptionSetParameter[LocalTime]("time", _.format(bpTimeFormatter), sqlType = Types.TIME)

    implicit val getLocalDateTime = mkGetResult(_.nextLocalDateTime())
    implicit val getLocalDateTimeOption = mkGetResult(_.nextLocalDateTimeOption())
    implicit val setLocalDateTime = mkSetParameter[LocalDateTime]("timestamp", _.format(bpDateTimeFormatter), sqlType = Types.TIMESTAMP)
    implicit val setLocalDateTimeOption = mkOptionSetParameter[LocalDateTime]("timestamp", _.format(bpDateTimeFormatter), sqlType = Types.TIMESTAMP)

    implicit val getOffsetTime = mkGetResult(_.nextOffsetTime())
    implicit val getOffsetTimeOption = mkGetResult(_.nextOffsetTimeOption())
    implicit val setOffsetTime = mkSetParameter[OffsetTime]("timetz", _.format(bpTzTimeFormatter), sqlType = Types.TIME /*Types.TIME_WITH_TIMEZONE*/)
    implicit val setOffsetTimeOption = mkOptionSetParameter[OffsetTime]("timetz", _.format(bpTzTimeFormatter), sqlType = Types.TIME /*Types.TIME_WITH_TIMEZONE*/)

    implicit val getOffsetDateTime = mkGetResult(_.nextOffsetDateTime())
    implicit val getOffsetDateTimeOption = mkGetResult(_.nextOffsetDateTimeOption())
    implicit val setOffsetDateTime = mkSetParameter[OffsetDateTime]("timestamptz", _.format(bpTzDateTimeFormatter), sqlType = Types.TIMESTAMP /*Types.TIMESTAMP_WITH_TIMEZONE*/)
    implicit val setOffsetDateTimeOption = mkOptionSetParameter[OffsetDateTime]("timestamptz", _.format(bpTzDateTimeFormatter), sqlType = Types.TIMESTAMP /*Types.TIMESTAMP_WITH_TIMEZONE*/)

    implicit val getZonedDateTime = mkGetResult(_.nextZonedDateTime())
    implicit val getZonedDateTimeOption = mkGetResult(_.nextZonedDateTimeOption())
    implicit val setZonedDateTime = mkSetParameter[ZonedDateTime]("timestamptz", _.format(bpTzDateTimeFormatter), sqlType = Types.TIMESTAMP /*Types.TIMESTAMP_WITH_TIMEZONE*/)
    implicit val setZonedDateTimeOption = mkOptionSetParameter[ZonedDateTime]("timestamptz", _.format(bpTzDateTimeFormatter), sqlType = Types.TIMESTAMP /*Types.TIMESTAMP_WITH_TIMEZONE*/)

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
