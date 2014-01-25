package com.github.tminglei.slickpg
package date

import scala.slick.driver.{PostgresDriver, JdbcTypesComponent}
import java.sql.{Timestamp, Time, Date}
import java.util.Calendar
import javax.xml.bind.DatatypeConverter
import org.postgresql.jdbc2.TimestampUtils
import org.postgresql.util.PGobject
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.slick.jdbc.{PositionedResult, PositionedParameters}
import scala.reflect.ClassTag
import java.lang.reflect.{Field, Method}

trait PgDateJavaTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  class DateJdbcType[DATE](fnFromDate: (Date => DATE),
                           fnToDate: (DATE => Date))(
                     implicit tag: ClassTag[DATE]) extends JdbcType[DATE] with BaseTypedType[DATE] {

    def scalaType: ScalaType[DATE] = ScalaBaseType[DATE]

    def zero: DATE = null.asInstanceOf[DATE]

    def sqlType: Int = java.sql.Types.DATE

    def sqlTypeName: String = "date"

    def setValue(v: DATE, p: PositionedParameters) = p.setDate(fnToDate(v))

    def setOption(v: Option[DATE], p: PositionedParameters) = p.setDateOption(v.map(fnToDate))

    def nextValue(r: PositionedResult): DATE = r.nextDateOption().map(fnFromDate).getOrElse(zero)

    def updateValue(v: DATE, r: PositionedResult) = r.updateDate(fnToDate(v))

    def hasLiteralForm: Boolean = true

    override def valueToSQLLiteral(v: DATE) = s"{d '${fnToDate(v)}'}"

  }

  ///
  class TimeJdbcType[TIME](fnFromTime: (Time => TIME),
                           fnToTime: (TIME => Time))(
                     implicit tag: ClassTag[TIME]) extends JdbcType[TIME] with BaseTypedType[TIME] {

    def scalaType: ScalaType[TIME] = ScalaBaseType[TIME]

    def zero: TIME = null.asInstanceOf[TIME]

    def sqlType: Int = java.sql.Types.TIME

    def sqlTypeName: String = "time"

    def setValue(v: TIME, p: PositionedParameters) = p.setTime(fnToTime(v))

    def setOption(v: Option[TIME], p: PositionedParameters) = p.setTimeOption(v.map(fnToTime))

    def nextValue(r: PositionedResult): TIME = r.nextTimeOption().map(fnFromTime).getOrElse(zero)

    def updateValue(v: TIME, r: PositionedResult) = r.updateTime(fnToTime(v))

    def hasLiteralForm: Boolean = true

    override def valueToSQLLiteral(v: TIME) = s"{t '${fnToTime(v)}'}"

  }

  ///
  class TimestampJdbcType[TIMESTAMP](fnFromTimestamp: (Timestamp => TIMESTAMP),
                                     fnToTimestamp: (TIMESTAMP => Timestamp))(
                      implicit tag: ClassTag[TIMESTAMP]) extends JdbcType[TIMESTAMP] with BaseTypedType[TIMESTAMP] {

    def scalaType: ScalaType[TIMESTAMP] = ScalaBaseType[TIMESTAMP]

    def zero: TIMESTAMP = null.asInstanceOf[TIMESTAMP]

    def sqlType: Int = java.sql.Types.TIMESTAMP

    def sqlTypeName: String = "timestamp"

    def setValue(v: TIMESTAMP, p: PositionedParameters) = p.setTimestamp(fnToTimestamp(v))

    def setOption(v: Option[TIMESTAMP], p: PositionedParameters) = p.setTimestampOption(v.map(fnToTimestamp))

    def nextValue(r: PositionedResult): TIMESTAMP = r.nextTimestampOption().map(fnFromTimestamp).getOrElse(zero)

    def updateValue(v: TIMESTAMP, r: PositionedResult) = r.updateTimestamp(fnToTimestamp(v))

    def hasLiteralForm: Boolean = true

    override def valueToSQLLiteral(v: TIMESTAMP) = s"{ts '${fnToTimestamp(v)}'}"

  }

  ///
  class TimestampTZJdbcType[TIMESTAMP_TZ](fnFromCalendar: (Calendar => TIMESTAMP_TZ),
                                      fnToCalendar: (TIMESTAMP_TZ => Calendar))(
                      implicit tag: ClassTag[TIMESTAMP_TZ]) extends JdbcType[TIMESTAMP_TZ] with BaseTypedType[TIMESTAMP_TZ] {

    def scalaType: ScalaType[TIMESTAMP_TZ] = ScalaBaseType[TIMESTAMP_TZ]

    def zero: TIMESTAMP_TZ = null.asInstanceOf[TIMESTAMP_TZ]

    def sqlType: Int = java.sql.Types.OTHER

    def sqlTypeName: String = "timestamptz"

    def setValue(v: TIMESTAMP_TZ, p: PositionedParameters) =
      p.setObject(mkPgObject(fnToCalendar(v)), sqlType)

    def setOption(v: Option[TIMESTAMP_TZ], p: PositionedParameters) =
      p.setObjectOption(v.map(fnToCalendar).map(mkPgObject), sqlType)

    def nextValue(r: PositionedResult): TIMESTAMP_TZ = r.nextStringOption()
      .map(PgDateJavaTypes.parseCalendar).map(fnFromCalendar).getOrElse(zero)

    def updateValue(v: TIMESTAMP_TZ, r: PositionedResult) =
      r.updateObject(mkPgObject(fnToCalendar(v)))

    def hasLiteralForm: Boolean = true

    override def valueToSQLLiteral(v: TIMESTAMP_TZ) =
      s"{ts '${DatatypeConverter.printDateTime(fnToCalendar(v))}'}"

    ///
    private def mkPgObject(v: Calendar) = {
      val obj = new PGobject
      obj.setType(sqlTypeName)
      obj.setValue(DatatypeConverter.printDateTime(v))
      obj
    }

  }
}

object PgDateJavaTypes {

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
