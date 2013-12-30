package com.github.tminglei.slickpg
package utils

import org.postgresql.util.PGobject
import scala.slick.jdbc.{PositionedResult, PositionedParameters}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.slick.driver.{PostgresDriver, JdbcTypesComponent}
import scala.reflect.ClassTag

trait PgCommonJdbcTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  class GenericJdbcType[T](pgTypeName: String,
                           fnFromString: (String => T),
                           fnToString: (T => String) = ((r: T) => r.toString),
                           val hasLiteralForm: Boolean = true)(
                   implicit tag: ClassTag[T]) extends JdbcType[T] with BaseTypedType[T] {

    def scalaType: ScalaType[T] = ScalaBaseType[T]

    def zero: T = null.asInstanceOf[T]

    def sqlType: Int = java.sql.Types.OTHER

    def sqlTypeName: String = pgTypeName

    def setValue(v: T, p: PositionedParameters) = p.setObject(mkPgObject(v), sqlType)

    def setOption(v: Option[T], p: PositionedParameters) = p.setObjectOption(v.map(mkPgObject), sqlType)

    def nextValue(r: PositionedResult): T = r.nextStringOption().map(fnFromString).getOrElse(zero)

    def updateValue(v: T, r: PositionedResult) = r.updateObject(mkPgObject(v))

    override def valueToSQLLiteral(v: T) = fnToString(v)

    ///
    private def mkPgObject(v: T) = {
      val obj = new PGobject
      obj.setType(sqlTypeName)
      obj.setValue(valueToSQLLiteral(v))
      obj
    }
  }
}
