package com.github.tminglei.slickpg
package date

import scala.slick.lifted.{BaseTypeMapper, TypeMapperDelegate}
import scala.slick.driver.BasicProfile
import scala.slick.session.{PositionedResult, PositionedParameters}
import org.postgresql.util.PGobject

class IntervalTypeMapper[INTERVAL](fnFromString: (String => INTERVAL),
                                   fnToString: (INTERVAL => String) = ((r: INTERVAL) => r.toString))
              extends TypeMapperDelegate[INTERVAL] with BaseTypeMapper[INTERVAL] {

  def apply(v1: BasicProfile): TypeMapperDelegate[INTERVAL] = this

  //-----------------------------------------------------------------
  def zero: INTERVAL = null.asInstanceOf[INTERVAL]

  def sqlType: Int = java.sql.Types.OTHER

  def sqlTypeName: String = "interval"

  def setValue(v: INTERVAL, p: PositionedParameters) = p.setObject(mkPgObject(v), sqlType)

  def setOption(v: Option[INTERVAL], p: PositionedParameters) = p.setObjectOption(v.map(mkPgObject), sqlType)

  def nextValue(r: PositionedResult): INTERVAL = r.nextStringOption().map(fnFromString).getOrElse(zero)

  def updateValue(v: INTERVAL, r: PositionedResult) = r.updateObject(mkPgObject(v))

  override def valueToSQLLiteral(v: INTERVAL) = fnToString(v)

  ///
  private def mkPgObject(v: INTERVAL) = {
    val obj = new PGobject
    obj.setType(sqlTypeName)
    obj.setValue(valueToSQLLiteral(v))
    obj
  }
}
