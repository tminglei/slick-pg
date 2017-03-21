package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import java.sql.{Date, Time, Timestamp}
import javax.xml.bind.DatatypeConverter
import java.util.Calendar

import slick.jdbc.JdbcType

trait PgDateSupport extends date.PgDateExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._

  /// alias
  trait DateTimeImplicits extends SimpleDateTimeImplicits

  trait SimpleDateTimeImplicits {
    implicit val simpleIntervalTypeMapper: JdbcType[Interval] = new GenericJdbcType[Interval]("interval", Interval.apply, hasLiteralForm=false)
    implicit val simpleTimestampTZTypeMapper: JdbcType[Calendar] = new GenericJdbcType[Calendar]("timestamptz",
        PgDateSupportUtils.parseCalendar, DatatypeConverter.printDateTime, hasLiteralForm=false)

    ///
    implicit def simpleDateColumnExtensionMethods(c: Rep[Date]) =
      new DateColumnExtensionMethods[Date, Time, Timestamp,  Interval, Date](c)
    implicit def simpleDateOptColumnExtensionMethods(c: Rep[Option[Date]]) =
      new DateColumnExtensionMethods[Date, Time, Timestamp, Interval, Option[Date]](c)

    implicit def simpleTimeColumnExtensionMethods(c: Rep[Time]) =
      new TimeColumnExtensionMethods[Date, Time, Timestamp, Calendar, Interval, Time](c)
    implicit def simpleTimeOptColumnExtensionMethods(c: Rep[Option[Time]]) =
      new TimeColumnExtensionMethods[Date, Time, Timestamp, Calendar, Interval, Option[Time]](c)

    implicit def simpleTimestampColumnExtensionMethods(c: Rep[Timestamp]) =
      new TimestampColumnExtensionMethods[Date, Time, Timestamp, Calendar, Interval, Timestamp](c)
    implicit def simpleTimestampOptColumnExtensionMethods(c: Rep[Option[Timestamp]]) =
      new TimestampColumnExtensionMethods[Date, Time, Timestamp, Calendar, Interval, Option[Timestamp]](c)

    implicit def simpleIntervalColumnExtensionMethods(c: Rep[Interval]) =
      new IntervalColumnExtensionMethods[Date, Time, Timestamp, Interval, Interval](c)
    implicit def simpleIntervalOptColumnExtensionMethods(c: Rep[Option[Interval]]) =
      new IntervalColumnExtensionMethods[Date, Time, Timestamp, Interval, Option[Interval]](c)

    implicit def simpleTimestampTZColumnExtensionMethods(c: Rep[Calendar]) =
      new TimestampColumnExtensionMethods[Date, Time, Calendar, Timestamp, Interval, Calendar](c)
    implicit def simpleTimestampTZOptColumnExtensionMethods(c: Rep[Option[Calendar]]) =
      new TimestampColumnExtensionMethods[Date, Time, Calendar, Timestamp, Interval, Option[Calendar]](c)
  }
}

object PgDateSupportUtils {
  import org.postgresql.jdbc2.TimestampUtils
  import java.lang.reflect.Field

  /** related codes hacked from [[org.postgresql.jdbc2.TimestampUtils]] */
  def parseCalendar(tsStr: String): Calendar = {

    val parsedts =
      if (tsUtil_loadCalendar.getName == "loadCalendar") tsUtil_loadCalendar.invoke(tsUtil_instance, null, tsStr, "timestamp")
      else /* parseBackendTimestamp(str) */ tsUtil_loadCalendar.invoke(tsUtil_instance, tsStr)

    val (tz, era, year, month, day, hour, minute, second, nanos) = tsUtil_getters(parsedts)
    val usedCal: Calendar = if (tz.get(parsedts) == null) Calendar.getInstance() else tz.get(parsedts).asInstanceOf[Calendar]

    usedCal.set(Calendar.ERA, era.get(parsedts).asInstanceOf[Int])
    usedCal.set(Calendar.YEAR, year.get(parsedts).asInstanceOf[Int])
    usedCal.set(Calendar.MONTH, month.get(parsedts).asInstanceOf[Int] - 1)
    usedCal.set(Calendar.DAY_OF_MONTH, day.get(parsedts).asInstanceOf[Int])
    usedCal.set(Calendar.HOUR_OF_DAY, hour.get(parsedts).asInstanceOf[Int])
    usedCal.set(Calendar.MINUTE, minute.get(parsedts).asInstanceOf[Int])
    usedCal.set(Calendar.SECOND, second.get(parsedts).asInstanceOf[Int])
    usedCal.set(Calendar.MILLISECOND, nanos.get(parsedts).asInstanceOf[Int] / 1000000)

    usedCal
  }

  //////////////////////////////////////////////////////////////////////

  private def tsUtil_instance = {
    import java.lang.Boolean.TRUE
    val tsUtilConstructor = classOf[TimestampUtils].getDeclaredConstructor(classOf[Boolean], classOf[Boolean], classOf[Boolean])
    tsUtilConstructor.setAccessible(true)
    tsUtilConstructor.newInstance(TRUE, TRUE, TRUE)
  }

  private val tsUtil_loadCalendar = {
    val loadCalendar = classOf[TimestampUtils].getDeclaredMethods.find(m => m.getName == "loadCalendar" || m.getName == "parseBackendTimestamp").get
    loadCalendar.setAccessible(true)
    loadCalendar
  }

  var tsUtilParsedGetters_holder: (Field, Field, Field, Field, Field, Field, Field, Field, Field) = null
  private def tsUtil_getters(parsed: AnyRef) = {
    def getField(clazz: Class[_], name: String) = {
      val field = clazz.getDeclaredField(name)
      field.setAccessible(true)
      field
    }

    if (tsUtilParsedGetters_holder == null) {
      val clazz = parsed.getClass
      val tz = getField(clazz, "tz")
      val era = getField(clazz, "era")
      val year = getField(clazz, "year")
      val month = getField(clazz, "month")
      val day = getField(clazz, "day")
      val hour = getField(clazz, "hour")
      val minute = getField(clazz, "minute")
      val second = getField(clazz, "second")
      val nanos = getField(clazz, "nanos")

      tsUtilParsedGetters_holder = (tz, era, year, month, day, hour, minute, second, nanos)
    }
    tsUtilParsedGetters_holder
  }
}

/**
 * copy from [[org.postgresql.util.PGInterval]],
 * should be more convenient to be used in scala environment
 */
import java.text.DecimalFormat
import org.postgresql.util.PGInterval

case class Interval(
  years: Int,
  months: Int,
  days: Int,
  hours: Int,
  minutes: Int,
  seconds: Double) {

  def milliseconds: Int = (microseconds + (if (microseconds < 0) -500 else 500)) / 1000
  def microseconds: Int = (seconds * 1000000.0).asInstanceOf[Int]

  def +:(cal: Calendar): Calendar = {
    cal.add(Calendar.MILLISECOND, milliseconds)
    cal.add(Calendar.MINUTE, minutes)
    cal.add(Calendar.HOUR, hours)
    cal.add(Calendar.DAY_OF_MONTH, days)
    cal.add(Calendar.MONTH, months)
    cal.add(Calendar.YEAR, years)
    cal
  }

  def +:(date: java.util.Date): java.util.Date = {
    val cal = Calendar.getInstance
    cal.setTime(date)
    date.setTime((cal +: this).getTime.getTime)
    date
  }

  def +(other: Interval): Interval = {
    new Interval(
      years + other.years,
      months + other.months,
      days + other.days,
      hours + other.hours,
      minutes + other.minutes,
      seconds + other.seconds
    )
  }

  def *(factor: Int): Interval = {
    new Interval(
      years * factor,
      months * factor,
      days * factor,
      hours * factor,
      minutes * factor,
      seconds * factor
    )
  }

  override def toString = {
    val secs = Interval.secondsFormat.format(seconds)
    ""+years+" years "+months+" mons "+days+" days "+hours+" hours "+minutes+" mins "+secs+" secs"
  }
}

object Interval {
  private val secondsFormat = {
    val format = new DecimalFormat("0.00####")
    val dfs = format.getDecimalFormatSymbols()
    dfs.setDecimalSeparator('.')
    format.setDecimalFormatSymbols(dfs)
    format
  }

  def apply(interval: String): Interval = fromPgInterval(new PGInterval(interval))

  def fromPgInterval(interval: PGInterval): Interval = {
    new Interval(
      interval.getYears,
      interval.getMonths,
      interval.getDays,
      interval.getHours,
      interval.getMinutes,
      interval.getSeconds
    )
  }

  def toPgInterval(interval: Interval): PGInterval = {
    new PGInterval(
      interval.years,
      interval.months,
      interval.days,
      interval.hours,
      interval.minutes,
      interval.seconds
    )
  }
}
