package com.github.tminglei.slickpg
package utils

import org.postgresql.util.PGobject
import scala.slick.driver.{PostgresDriver, JdbcTypesComponent}
import scala.reflect.ClassTag
import java.sql.{PreparedStatement, ResultSet}

trait PgCommonJdbcTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  class GenericJdbcType[T](override val sqlTypeName: String,
                           fnFromString: (String => T),
                           fnToString: (T => String) = ((r: T) => r.toString),
                           val sqlType: Int = java.sql.Types.OTHER,
                           zero: T = null.asInstanceOf[T],
                           override val hasLiteralForm: Boolean = false)(
                  implicit override val classTag: ClassTag[T]) extends DriverJdbcType[T] {

    override def getValue(r: ResultSet, idx: Int): T = {
      val value = r.getString(idx)
      if (r.wasNull) zero else fnFromString(value)
    }

    override def setValue(v: T, p: PreparedStatement, idx: Int): Unit = p.setObject(idx, mkPgObject(v))

    override def updateValue(v: T, r: ResultSet, idx: Int): Unit = r.updateObject(idx, mkPgObject(v))

    override def valueToSQLLiteral(v: T) = if(v == null) "NULL" else s"'${fnToString(v)}'"

    ///
    private def mkPgObject(v: T) = {
      val obj = new PGobject
      obj.setType(sqlTypeName)
      obj.setValue(if(v == null) null else fnToString(v))
      obj
    }
  }
}
