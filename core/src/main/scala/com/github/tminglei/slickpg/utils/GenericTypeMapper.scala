package com.github.tminglei.slickpg
package utils

import scala.slick.lifted.{BaseTypeMapper, TypeMapperDelegate}
import scala.slick.driver.BasicProfile
import scala.slick.session.{PositionedResult, PositionedParameters}
import org.postgresql.util.PGobject

class GenericTypeMapper[T](val sqlTypeName: String,
						   fnFromString: (String => T),
                           fnToString: (T => String) = ((r: T) => r.toString),
                           val sqlType: Int = java.sql.Types.OTHER,
                           val zero: T = null.asInstanceOf[T])
                extends TypeMapperDelegate[T] with BaseTypeMapper[T] {

  def apply(v1: BasicProfile): TypeMapperDelegate[T] = this

  //-----------------------------------------------------------------

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
