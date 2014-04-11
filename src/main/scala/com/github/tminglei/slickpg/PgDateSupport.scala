package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import java.sql.{Timestamp, Time, Date}
import javax.xml.bind.DatatypeConverter
import scala.slick.lifted.Column
import java.util.Calendar

trait PgDateSupport extends date.PgDateExtensions with date.PgDateJdbcTypes with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.Implicit._

  type DATE   = Date
  type TIME   = Time
  type TIMESTAMP = Timestamp
  type INTERVAL  = Interval
  
  type TIMESTAMP_TZ = Calendar

  trait DateTimeImplicits {
    implicit val intervalTypeMapper = new GenericJdbcType[Interval]("interval", Interval.apply, hasLiteralForm=false)
    implicit val timestampTZTypeMapper = new GenericJdbcType[Calendar]("timestamptz",
        PgDateJdbcTypeUtils.parseCalendar, DatatypeConverter.printDateTime, hasLiteralForm=false)

    ///
    implicit def dateColumnExtensionMethods(c: Column[Date]) = new DateColumnExtensionMethods(c)
    implicit def dateOptColumnExtensionMethods(c: Column[Option[Date]]) = new DateColumnExtensionMethods(c)

    implicit def timeColumnExtensionMethods(c: Column[Time]) = new TimeColumnExtensionMethods(c)
    implicit def timeOptColumnExtensionMethods(c: Column[Option[Time]]) = new TimeColumnExtensionMethods(c)

    implicit def timestampColumnExtensionMethods(c: Column[Timestamp]) = new TimestampColumnExtensionMethods(c)
    implicit def timestampOptColumnExtensionMethods(c: Column[Option[Timestamp]]) = new TimestampColumnExtensionMethods(c)

    implicit def intervalColumnExtensionMethods(c: Column[Interval]) = new IntervalColumnExtensionMethods(c)
    implicit def intervalOptColumnExtensionMethods(c: Column[Option[Interval]]) = new IntervalColumnExtensionMethods(c)

    implicit def timestampTZColumnExtensionMethods(c: Column[Calendar]) = new TimestampTZColumnExtensionMethods(c)
    implicit def timestampTZOptColumnExtensionMethods(c: Column[Option[Calendar]]) = new TimestampTZColumnExtensionMethods(c)
  }
}

object PgDateJdbcTypeUtils {
  import org.postgresql.jdbc2.TimestampUtils
  import java.lang.reflect.{Field, Method}

  /** related codes hacked from [[org.postgresql.jdbc2.TimestampUtils]] */
  def parseCalendar(tsStr: String): Calendar = {

    val ts = tsUtilLoadCalendar.invoke(tsUtilInstance, null, tsStr, "timestamp")

    val (tz, era, year, month, day, hour, minute, second, nanos) = tsUtilGetters(ts)
    val useCal: Calendar = if (tz.get(ts) == null) Calendar.getInstance() else tz.get(ts).asInstanceOf[Calendar]

    useCal.set(Calendar.ERA, era.get(ts).asInstanceOf[Int])
    useCal.set(Calendar.YEAR, year.get(ts).asInstanceOf[Int])
    useCal.set(Calendar.MONTH, month.get(ts).asInstanceOf[Int] - 1)
    useCal.set(Calendar.DAY_OF_MONTH, day.get(ts).asInstanceOf[Int])
    useCal.set(Calendar.HOUR_OF_DAY, hour.get(ts).asInstanceOf[Int])
    useCal.set(Calendar.MINUTE, minute.get(ts).asInstanceOf[Int])
    useCal.set(Calendar.SECOND, second.get(ts).asInstanceOf[Int])
    useCal.set(Calendar.MILLISECOND, nanos.get(ts).asInstanceOf[Int] / 1000)

    useCal
  }

  //////////////////////////////////////////////////////////////////////
  private val tsUtilInstanceHolder = new ThreadLocal[TimestampUtils]
  private val tsUtilLoadCalendarHolder = new ThreadLocal[Method]
  private val tsUtilParsedGettersHolder = new ThreadLocal[(Field, Field, Field, Field, Field, Field, Field, Field, Field)]

  private def tsUtilInstance = {
    import java.lang.Boolean.TRUE
    if (tsUtilInstanceHolder.get() == null) {
      val tsUtilConstructor = classOf[TimestampUtils].getDeclaredConstructor(classOf[Boolean], classOf[Boolean], classOf[Boolean])
      tsUtilConstructor.setAccessible(true)
      tsUtilInstanceHolder.set(tsUtilConstructor.newInstance(TRUE, TRUE, TRUE))
    }
    tsUtilInstanceHolder.get()
  }

  private def tsUtilLoadCalendar = {
    if (tsUtilLoadCalendarHolder.get() == null) {
      val loadCalendar = classOf[TimestampUtils].getDeclaredMethods.find(_.getName == "loadCalendar").get
      loadCalendar.setAccessible(true)
      tsUtilLoadCalendarHolder.set(loadCalendar)
    }
    tsUtilLoadCalendarHolder.get()
  }

  private def tsUtilGetters(parsed: AnyRef) = {
    def getField(clazz: Class[_], name: String) = {
      val field = clazz.getDeclaredField(name)
      field.setAccessible(true)
      field
    }

    if (tsUtilParsedGettersHolder.get() == null) {
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

      tsUtilParsedGettersHolder.set((tz, era, year, month, day, hour, minute, second, nanos))
    }
    tsUtilParsedGettersHolder.get()
  }
}
