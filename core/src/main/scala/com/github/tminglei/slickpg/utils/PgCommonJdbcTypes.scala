package com.github.tminglei.slickpg
package utils

import slick.ast.FieldSymbol
import slick.driver.{PostgresDriver, JdbcTypesComponent}
import slick.profile.RelationalProfile.ColumnOption.Length
import scala.reflect.ClassTag
import java.sql.{PreparedStatement, ResultSet}

trait PgCommonJdbcTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  class GenericJdbcType[T](val sqlTypeName: String,
                           fnFromString: (String => T),
                           fnToString: (T => String) = ((r: T) => r.toString),
                           val sqlType: Int = java.sql.Types.OTHER,
                           zero: T = null.asInstanceOf[T],
                           override val hasLiteralForm: Boolean = false)(
                  implicit override val classTag: ClassTag[T]) extends DriverJdbcType[T] {

    override def sqlTypeName(sym: Option[FieldSymbol]): String = sqlTypeName

    override def getValue(r: ResultSet, idx: Int): T = {
      val value = r.getString(idx)
      if (r.wasNull) zero else fnFromString(value)
    }

    override def setValue(v: T, p: PreparedStatement, idx: Int): Unit = p.setObject(idx, fnToString(v))

    override def updateValue(v: T, r: ResultSet, idx: Int): Unit = r.updateObject(idx, fnToString(v))

    override def valueToSQLLiteral(v: T) = if(v == null) "NULL" else s"'${fnToString(v)}'"
  }
}
