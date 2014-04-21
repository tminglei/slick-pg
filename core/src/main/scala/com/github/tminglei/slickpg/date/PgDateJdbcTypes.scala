package com.github.tminglei.slickpg
package date

import scala.slick.driver.{PostgresDriver, JdbcTypesComponent}
import java.sql.{Timestamp, Time, Date}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.slick.jdbc.{PositionedResult, PositionedParameters}
import scala.reflect.ClassTag

trait PgDateJdbcTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  class DateJdbcType[DATE : ClassTag](
              fnFromDate: (Date => DATE),
              fnToDate: (DATE => Date)) extends JdbcType[DATE] with BaseTypedType[DATE] {

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
  class TimeJdbcType[TIME : ClassTag](
              fnFromTime: (Time => TIME),
              fnToTime: (TIME => Time)) extends JdbcType[TIME] with BaseTypedType[TIME] {

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
  class TimestampJdbcType[TIMESTAMP : ClassTag](
              fnFromTimestamp: (Timestamp => TIMESTAMP),
              fnToTimestamp: (TIMESTAMP => Timestamp)) extends JdbcType[TIMESTAMP] with BaseTypedType[TIMESTAMP] {

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
}
