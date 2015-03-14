package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import org.joda.time._
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormat}
import slick.jdbc.{SetParameter, PositionedParameters, PositionedResult}
import org.postgresql.util.PGInterval

trait PgDateSupportJoda extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import PgJodaSupportUtils._

  /// alias
  trait DateTimeImplicits extends JodaDateTimeImplicits

  trait JodaDateTimeFormatters {
    val jodaDateFormatter = ISODateTimeFormat.date()
    val jodaTimeFormatter = DateTimeFormat.forPattern("HH:mm:ss.SSSSSS")
    val jodaTimeFormatter_NoFraction = DateTimeFormat.forPattern("HH:mm:ss")
    val jodaDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
    val jodaDateTimeFormatter_NoFraction = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val jodaTzDateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZ")
    val jodaTzDateTimeFormatter_NoFraction = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ssZ")
  }

  trait JodaDateTimeImplicits extends JodaDateTimeFormatters {
    implicit val jodaDateTypeMapper = new GenericJdbcType[LocalDate]("date",
      LocalDate.parse(_, jodaDateFormatter), _.toString(jodaDateFormatter), hasLiteralForm=false)
    implicit val jodaTimeTypeMapper = new GenericJdbcType[LocalTime]("time",
      fnFromString = (s) => LocalTime.parse(s, if(s.indexOf(".") > 0 ) jodaTimeFormatter else jodaTimeFormatter_NoFraction),
      fnToString = (v) => v.toString(jodaTimeFormatter),
      hasLiteralForm = false)
    implicit val jodaDateTimeTypeMapper = new GenericJdbcType[LocalDateTime]("timestamp",
      fnFromString = (s) => LocalDateTime.parse(s, if(s.indexOf(".") > 0 ) jodaDateTimeFormatter else jodaDateTimeFormatter_NoFraction),
      fnToString = (v) => v.toString(jodaDateTimeFormatter),
      hasLiteralForm = false)
    implicit val jodaPeriodTypeMapper = new GenericJdbcType[Period]("interval",
      pgIntervalStr2jodaPeriod, hasLiteralForm=false)
    implicit val jodaTimestampTZTypeMapper = new GenericJdbcType[DateTime]("timestamptz",
      fnFromString = (s) => DateTime.parse(s, if(s.indexOf(".") > 0 ) jodaTzDateTimeFormatter else jodaTzDateTimeFormatter_NoFraction),
      fnToString = (v) => v.toString(jodaTzDateTimeFormatter),
      hasLiteralForm = false)

    ///
    implicit def jodaDateColumnExtensionMethods(c: Rep[LocalDate]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, LocalDate](c)
    implicit def jodaDateOptColumnExtensionMethods(c: Rep[Option[LocalDate]]) =
      new DateColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[LocalDate]](c)

    implicit def jodaTimeColumnExtensionMethods(c: Rep[LocalTime]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, LocalTime](c)
    implicit def jodaTimeOptColumnExtensionMethods(c: Rep[Option[LocalTime]]) =
      new TimeColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[LocalTime]](c)

    implicit def jodaTimestampColumnExtensionMethods(c: Rep[LocalDateTime]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, LocalDateTime](c)
    implicit def jodaTimestampOptColumnExtensionMethods(c: Rep[Option[LocalDateTime]]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[LocalDateTime]](c)

    implicit def jodaIntervalColumnExtensionMethods(c: Rep[Period]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Period](c)
    implicit def jodaIntervalOptColumnExtensionMethods(c: Rep[Option[Period]]) =
      new IntervalColumnExtensionMethods[LocalDate, LocalTime, LocalDateTime, Period, Option[Period]](c)

    implicit def jodaTzTimestampColumnExtensionMethods(c: Rep[DateTime]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, DateTime, Period, DateTime](c)
    implicit def jodaTzTimestampOptColumnExtensionMethods(c: Rep[Option[DateTime]]) =
      new TimestampColumnExtensionMethods[LocalDate, LocalTime, DateTime, Period, Option[DateTime]](c)
  }

  trait JodaDateTimePlainImplicits extends JodaDateTimeFormatters {
    import java.sql.Types

    implicit class PgDate2TimePositionedResult(r: PositionedResult) {
      def nextLocalDate() = nextLocalDateOption().orNull
      def nextLocalDateOption() = r.nextStringOption().map(LocalDate.parse(_, jodaDateFormatter))
      def nextLocalTime() = nextLocalTimeOption().orNull
      def nextLocalTimeOption() = r.nextStringOption().map(s => LocalTime.parse(s,
        if(s.indexOf(".") > 0 ) jodaTimeFormatter else jodaTimeFormatter_NoFraction))
      def nextLocalDateTime() = nextLocalDateTimeOption().orNull
      def nextLocalDateTimeOption() = r.nextStringOption().map(s => LocalDateTime.parse(s,
        if(s.indexOf(".") > 0 ) jodaDateTimeFormatter else jodaDateTimeFormatter_NoFraction))
      def nextZonedDateTime() = nextZonedDateTimeOption().orNull
      def nextZonedDateTimeOption() = r.nextStringOption().map(s => DateTime.parse(s,
        if(s.indexOf(".") > 0 ) jodaTzDateTimeFormatter else jodaTzDateTimeFormatter_NoFraction))
      def nextPeriod() = nextPeriodOption().orNull
      def nextPeriodOption() = r.nextStringOption().map(pgIntervalStr2jodaPeriod)
    }

    /////////////////////////////////////////////////////////////////////////////
    implicit object SetLocalDate extends SetParameter[LocalDate] {
      def apply(v: LocalDate, pp: PositionedParameters) = setDateTime(Types.DATE, "date", Option(v).map(_.toString(jodaDateFormatter)), pp)
    }
    implicit object SetLocalDateOption extends SetParameter[Option[LocalDate]] {
      def apply(v: Option[LocalDate], pp: PositionedParameters) = setDateTime(Types.DATE, "date", v.map(_.toString(jodaDateFormatter)), pp)
    }
    ///
    implicit object SetLocalTime extends SetParameter[LocalTime] {
      def apply(v: LocalTime, pp: PositionedParameters) = setDateTime(Types.DATE, "time", Option(v).map(_.toString(jodaTimeFormatter)), pp)
    }
    implicit object SetLocalTimeOption extends SetParameter[Option[LocalTime]] {
      def apply(v: Option[LocalTime], pp: PositionedParameters) = setDateTime(Types.DATE, "time", v.map(_.toString(jodaTimeFormatter)), pp)
    }
    ///
    implicit object SetLocalDateTime extends SetParameter[LocalDateTime] {
      def apply(v: LocalDateTime, pp: PositionedParameters) = setDateTime(Types.TIMESTAMP, "timestamp", Option(v).map(_.toString(jodaDateTimeFormatter)), pp)
    }
    implicit object SetLocalDateTimeOption extends SetParameter[Option[LocalDateTime]] {
      def apply(v: Option[LocalDateTime], pp: PositionedParameters) = setDateTime(Types.TIMESTAMP, "timestamp", v.map(_.toString(jodaDateTimeFormatter)), pp)
    }
    ///
    implicit object SetZonedDateTime extends SetParameter[DateTime] {
      def apply(v: DateTime, pp: PositionedParameters) = setDateTime(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", Option(v).map(_.toString(jodaTzDateTimeFormatter)), pp)
    }
    implicit object SetZonedDateTimeOption extends SetParameter[Option[DateTime]] {
      def apply(v: Option[DateTime], pp: PositionedParameters) = setDateTime(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz", v.map(_.toString(jodaTzDateTimeFormatter)), pp)
    }
    ///
    implicit object SetPeriod extends SetParameter[Period] {
      def apply(v: Period, pp: PositionedParameters) = setDateTime(Types.OTHER, "interval", Option(v).map(_.toString), pp)
    }
    implicit object SetPeriodOption extends SetParameter[Option[Period]] {
      def apply(v: Option[Period], pp: PositionedParameters) = setDateTime(Types.OTHER, "interval", v.map(_.toString), pp)
    }

    ///
    private def setDateTime(sqlType: Int, typeName: String, v: => Option[String], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject(typeName, v), Types.OTHER)
      case None    => p.setNull(sqlType)
    }
  }
}

object PgJodaSupportUtils {
  /// pg interval string --> joda Duration
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
