package com.github.tminglei.slickpg
package date

import scala.slick.driver.{PostgresDriver, JdbcTypesComponent}
import java.sql.{PreparedStatement, ResultSet, Date, Time, Timestamp}
import scala.reflect.ClassTag

trait PgDateJdbcTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  class DateJdbcType[DATE](fnFromDate: (Date => DATE),
                           fnToDate: (DATE => Date))(
                implicit override val classTag: ClassTag[DATE]) extends DriverJdbcType[DATE] {

    override def sqlType: Int = java.sql.Types.DATE

    override def getValue(r: ResultSet, idx: Int): DATE = {
      val value = r.getDate(idx)
      if (r.wasNull) null.asInstanceOf[DATE] else fnFromDate(value)
    }

    override def setValue(v: DATE, p: PreparedStatement, idx: Int): Unit = p.setDate(idx, fnToDate(v))

    override def updateValue(v: DATE, r: ResultSet, idx: Int): Unit = r.updateDate(idx, fnToDate(v))

    override def valueToSQLLiteral(v: DATE) = s"{d '${fnToDate(v)}'}"
  }

  ///
  class TimeJdbcType[TIME](fnFromTime: (Time => TIME),
                           fnToTime: (TIME => Time))(
                implicit override val classTag: ClassTag[TIME]) extends DriverJdbcType[TIME] {

    override def sqlType: Int = java.sql.Types.TIME

    override def getValue(r: ResultSet, idx: Int): TIME = {
      val value = r.getTime(idx)
      if (r.wasNull) null.asInstanceOf[TIME] else fnFromTime(value)
    }

    override def setValue(v: TIME, p: PreparedStatement, idx: Int): Unit = p.setTime(idx, fnToTime(v))

    override def updateValue(v: TIME, r: ResultSet, idx: Int): Unit = r.updateTime(idx, fnToTime(v))

    override def valueToSQLLiteral(v: TIME) = s"{t '${fnToTime(v)}'}"
  }

  ///
  class TimestampJdbcType[TIMESTAMP](fnFromTimestamp: (Timestamp => TIMESTAMP),
                                     fnToTimestamp: (TIMESTAMP => Timestamp))(
                  implicit override val classTag: ClassTag[TIMESTAMP]) extends DriverJdbcType[TIMESTAMP] {

    override def sqlType: Int = java.sql.Types.TIMESTAMP

    override def getValue(r: ResultSet, idx: Int): TIMESTAMP = {
      val value = r.getTimestamp(idx)
      if (r.wasNull) null.asInstanceOf[TIMESTAMP] else fnFromTimestamp(value)
    }

    override def setValue(v: TIMESTAMP, p: PreparedStatement, idx: Int): Unit = p.setTimestamp(idx, fnToTimestamp(v))

    override def updateValue(v: TIMESTAMP, r: ResultSet, idx: Int): Unit = r.updateTimestamp(idx, fnToTimestamp(v))

    override def valueToSQLLiteral(v: TIMESTAMP) = s"{ts '${fnToTimestamp(v)}'}"
  }
}
