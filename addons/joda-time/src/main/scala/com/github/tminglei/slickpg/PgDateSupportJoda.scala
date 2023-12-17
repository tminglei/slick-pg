package com.github.tminglei.slickpg

import org.joda.time._
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat}
import org.postgresql.util.PGInterval
import slick.jdbc.{GetResult, JdbcType, PositionedResult, PostgresProfile, SetParameter}

import scala.reflect.classTag

trait PgDateSupportJoda extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._
  import PgJodaSupportUtils._

  trait JodaTimeCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("date", classTag[LocalDate])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("time", classTag[LocalTime])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("timestamp", classTag[LocalDateTime])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("timestamptz", classTag[DateTime])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("interval", classTag[Period])
    }
  }

  trait JodaDateTimeFormatters {
    val jodaDateFormatter = ISODateTimeFormat.date()
    val jodaTimeFormatter = DateTimeFormat.forPattern("HH:mm:ss.SSSSSS")
    val jodaTimeFormatter_NoFraction = DateTimeFormat.forPattern("HH:mm:ss")
    val jodaDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
    val jodaDateTimeFormatter_NoFraction = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val jodaTzTimeFormatter = DateTimeFormat.forPattern("HH:mm:ss.SSSSSSZ")
    val jodaTzTimeFormatter_NoFraction = DateTimeFormat.forPattern("HH:mm:ssZ")
    val jodaTzDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")
    val jodaTzDateTimeFormatter_NoFraction = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ")

    private[PgDateSupportJoda] def timeFormatter(s: String) =
      if(s.indexOf(".") > 0 ) jodaTimeFormatter else jodaTimeFormatter_NoFraction
    private[PgDateSupportJoda] def dateTimeFormatter(s: String) =
      if(s.indexOf(".") > 0 ) jodaDateTimeFormatter else jodaDateTimeFormatter_NoFraction
    private[PgDateSupportJoda] def tzDateTimeFormatter(s: String) = if (s.indexOf(":") > 2) {
      if (s.indexOf(".") > 0) jodaTzDateTimeFormatter else jodaTzDateTimeFormatter_NoFraction
    } else {
      if (s.indexOf(".") > 0) jodaTzTimeFormatter else jodaTzTimeFormatter_NoFraction
    }
  }

  trait JodaDateTimeImplicits extends JodaDateTimeFormatters with JodaTimeCodeGenSupport {
    implicit val jodaDateTypeMapper: JdbcType[LocalDate] = new GenericJdbcType[LocalDate]("date",
      LocalDate.parse(_, jodaDateFormatter), _.toString(jodaDateFormatter), hasLiteralForm=false)
    implicit val jodaTimeTypeMapper: JdbcType[LocalTime] = new GenericJdbcType[LocalTime]("time",
      fnFromString = (s) => LocalTime.parse(s, timeFormatter(s)),
      fnToString = (v) => v.toString(jodaTimeFormatter),
      hasLiteralForm = false)
    implicit val jodaDateTimeTypeMapper: JdbcType[LocalDateTime] = new GenericJdbcType[LocalDateTime]("timestamp",
      fnFromString = (s) => LocalDateTime.parse(s, dateTimeFormatter(s)),
      fnToString = (v) => v.toString(jodaDateTimeFormatter),
      hasLiteralForm = false)
    implicit val jodaPeriodTypeMapper: JdbcType[Period] = new GenericJdbcType[Period]("interval",
      pgIntervalStr2jodaPeriod, hasLiteralForm=false)
    implicit val jodaTimestampTZTypeMapper: JdbcType[DateTime] = new GenericJdbcType[DateTime]("timestamptz",
      fnFromString = (s) => DateTime.parse(s, tzDateTimeFormatter(s)),
      fnToString = (v) => v.toString(jodaTzDateTimeFormatter),
      hasLiteralForm = false)
    implicit val jodaInstantTypeMapper: JdbcType[Instant] = new GenericJdbcType[Instant]("timestamptz",
      fnFromString = (s) => Instant.parse(s, tzDateTimeFormatter(s)),
      fnToString = (v) => v.toString(jodaTzDateTimeFormatter),
      hasLiteralForm = false)

    ///
    implicit def jodaDateColumnExtensionMethods(c: Rep[LocalDate]): DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, LocalDate] =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, LocalDate](c)
    implicit def jodaDateOptColumnExtensionMethods(c: Rep[Option[LocalDate]]): DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[LocalDate]] =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[LocalDate]](c)

    implicit def jodaTimeColumnExtensionMethods(c: Rep[LocalTime]): TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, LocalTime] =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, LocalTime](c)
    implicit def jodaTimeOptColumnExtensionMethods(c: Rep[Option[LocalTime]]): TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, Option[LocalTime]] =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, Option[LocalTime]](c)

    implicit def jodaTimestampColumnExtensionMethods(c: Rep[LocalDateTime]): TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, LocalDateTime] =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, LocalDateTime](c)
    implicit def jodaTimestampOptColumnExtensionMethods(c: Rep[Option[LocalDateTime]]): TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, Option[LocalDateTime]] =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, DateTime, Period, Option[LocalDateTime]](c)

    implicit def jodaIntervalColumnExtensionMethods(c: Rep[Period]): IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Period] =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Period](c)
    implicit def jodaIntervalOptColumnExtensionMethods(c: Rep[Option[Period]]): IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[Period]] =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[Period]](c)

    implicit def jodaTzTimestampColumnExtensionMethods(c: Rep[DateTime]): TimestampColumnExtensionMethods[LocalDate, LocalTime, DateTime, LocalDateTime, Period, DateTime] =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, DateTime, LocalDateTime, Period, DateTime](c)
    implicit def jodaTzTimestampOptColumnExtensionMethods(c: Rep[Option[DateTime]]): TimestampColumnExtensionMethods[LocalDate, LocalTime, DateTime, LocalDateTime, Period, Option[DateTime]] =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, DateTime, LocalDateTime, Period, Option[DateTime]](c)

    implicit def jodaTimestamp1ColumnExtensionMethods(c: Rep[Instant]): TimestampColumnExtensionMethods[LocalDate, LocalTime, Instant, LocalDateTime, Period, Instant] =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, Instant, LocalDateTime, Period, Instant](c)
    implicit def jodaTimestamp1OptColumnExtensionMethods(c: Rep[Option[Instant]]): TimestampColumnExtensionMethods[LocalDate, LocalTime, Instant, LocalDateTime, Period, Option[Instant]] =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, Instant, LocalDateTime, Period, Option[Instant]](c)
  }

  trait JodaDateTimePlainImplicits extends JodaDateTimeFormatters with JodaTimeCodeGenSupport {
    import java.sql.Types
    import utils.PlainSQLUtils._

    implicit class PgJodaTimePositionedResult(r: PositionedResult) {
      def nextLocalDate() = nextLocalDateOption().orNull
      def nextLocalDateOption() = r.nextStringOption().map(LocalDate.parse(_, jodaDateFormatter))
      def nextLocalTime() = nextLocalTimeOption().orNull
      def nextLocalTimeOption() = r.nextStringOption().map(s => LocalTime.parse(s, timeFormatter(s)))
      def nextLocalDateTime() = nextLocalDateTimeOption().orNull
      def nextLocalDateTimeOption() = r.nextStringOption().map(s => LocalDateTime.parse(s, dateTimeFormatter(s)))
      def nextZonedDateTime() = nextZonedDateTimeOption().orNull
      def nextZonedDateTimeOption() = r.nextStringOption().map(s => DateTime.parse(s, tzDateTimeFormatter(s)))
      def nextPeriod() = nextPeriodOption().orNull
      def nextPeriodOption() = r.nextStringOption().map(pgIntervalStr2jodaPeriod)
      def nextInstant() = nextInstantOption().orNull
      def nextInstantOption() = r.nextStringOption().map(s => Instant.parse(s, tzDateTimeFormatter(s)))
    }

    /////////////////////////////////////////////////////////////////////////////
    implicit val getLocalDate: GetResult[LocalDate] = mkGetResult(_.nextLocalDate())
    implicit val getLocalDateOption: GetResult[Option[LocalDate]] = mkGetResult(_.nextLocalDateOption())
    implicit val setLocalDate: SetParameter[LocalDate] = mkSetParameter[LocalDate]("date", _.toString(jodaDateFormatter), sqlType = Types.DATE)
    implicit val setLocalDateOption: SetParameter[Option[LocalDate]] = mkOptionSetParameter[LocalDate]("date", _.toString(jodaDateFormatter), sqlType = Types.DATE)

    implicit val getLocalTime: GetResult[LocalTime] = mkGetResult(_.nextLocalTime())
    implicit val getLocalTimeOption: GetResult[Option[LocalTime]] = mkGetResult(_.nextLocalTimeOption())
    implicit val setLocalTime: SetParameter[LocalTime] = mkSetParameter[LocalTime]("time", _.toString(jodaTimeFormatter), sqlType = Types.TIME)
    implicit val setLocalTimeOption: SetParameter[Option[LocalTime]] = mkOptionSetParameter[LocalTime]("time", _.toString(jodaTimeFormatter), sqlType = Types.TIME)

    implicit val getLocalDateTime: GetResult[LocalDateTime] = mkGetResult(_.nextLocalDateTime())
    implicit val getLocalDateTimeOption: GetResult[Option[LocalDateTime]] = mkGetResult(_.nextLocalDateTimeOption())
    implicit val setLocalDateTime: SetParameter[LocalDateTime] = mkSetParameter[LocalDateTime]("timestamp", _.toString(jodaDateTimeFormatter), sqlType = Types.TIMESTAMP)
    implicit val setLocalDateTimeOption: SetParameter[Option[LocalDateTime]] = mkOptionSetParameter[LocalDateTime]("timestamp", _.toString(jodaDateTimeFormatter), sqlType = Types.TIMESTAMP)

    implicit val getZonedDateTime: GetResult[DateTime] = mkGetResult(_.nextZonedDateTime())
    implicit val getZonedDateTimeOption: GetResult[Option[DateTime]] = mkGetResult(_.nextZonedDateTimeOption())
    implicit val setZonedDateTime: SetParameter[DateTime] = mkSetParameter[DateTime]("timestamptz", _.toString(jodaTzDateTimeFormatter), sqlType = Types.TIMESTAMP /*Types.TIMESTAMP_WITH_TIMEZONE*/)
    implicit val setZonedDateTimeOption: SetParameter[Option[DateTime]] = mkOptionSetParameter[DateTime]("timestamptz", _.toString(jodaTzDateTimeFormatter), sqlType = Types.TIMESTAMP /*Types.TIMESTAMP_WITH_TIMEZONE*/)

    implicit val getInstant: GetResult[Instant] = mkGetResult(_.nextInstant())
    implicit val getInstantOption: GetResult[Option[Instant]] = mkGetResult(_.nextInstantOption())
    implicit val setInstant: SetParameter[Instant] = mkSetParameter[Instant]("timestamptz", _.toString(jodaTzDateTimeFormatter), sqlType = Types.TIMESTAMP /*Types.TIMESTAMP_WITH_TIMEZONE*/)
    implicit val setInstantOption: SetParameter[Option[Instant]] = mkOptionSetParameter[Instant]("timestamptz", _.toString(jodaTzDateTimeFormatter), sqlType = Types.TIMESTAMP /*Types.TIMESTAMP_WITH_TIMEZONE*/)

    implicit val getPeriod: GetResult[Period] = mkGetResult(_.nextPeriod())
    implicit val getPeriodOption: GetResult[Option[Period]] = mkGetResult(_.nextPeriodOption())
    implicit val setPeriod: SetParameter[Period] = mkSetParameter[Period]("interval")
    implicit val setPeriodOption: SetParameter[Option[Period]] = mkOptionSetParameter[Period]("interval")

    implicit val setDuration: SetParameter[Duration] = mkSetParameter[Duration]("interval")
    implicit val setDurationOption: SetParameter[Option[Duration]] = mkOptionSetParameter[Duration]("interval")
  }
}

object PgJodaSupportUtils {
  /// pg interval string --> joda Period
  def pgIntervalStr2jodaPeriod(intervalStr: String): Period = {
    val pgInterval = new PGInterval(intervalStr)
    val seconds = Math.floor(pgInterval.getSeconds) .asInstanceOf[Int]
    val millis  = ((pgInterval.getSeconds - seconds) * 1000) .asInstanceOf[Int]

    new Period(
      pgInterval.getYears,
      pgInterval.getMonths,
      0,  // weeks
      pgInterval.getDays,
      pgInterval.getHours,
      pgInterval.getMinutes,
      seconds, millis
    )
  }
}
