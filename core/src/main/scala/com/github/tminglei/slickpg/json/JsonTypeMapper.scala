package com.github.tminglei.slickpg.json

import scala.slick.lifted.{BaseTypeMapper, TypeMapperDelegate}
import scala.slick.driver.BasicProfile
import scala.slick.session.{PositionedResult, PositionedParameters}
import org.postgresql.util.PGobject

class JsonTypeMapper[JSONTYPE](fnFromString: (String => JSONTYPE),
                               fnToString: (JSONTYPE => String) = ((r: JSONTYPE) => r.toString))
                extends TypeMapperDelegate[JSONTYPE] with BaseTypeMapper[JSONTYPE] {

  def apply(v1: BasicProfile): TypeMapperDelegate[JSONTYPE] = this

  //----------------------------------------------------------
  def zero: JSONTYPE = null.asInstanceOf[JSONTYPE]

  def sqlType = java.sql.Types.OTHER

  def sqlTypeName = "json"

  def setValue(v: JSONTYPE, p: PositionedParameters) = p.setObject(mkPgObject(v), sqlType)

  def setOption(v: Option[JSONTYPE], p: PositionedParameters) = p.setObjectOption(v.map(mkPgObject), sqlType)

  def nextValue(r: PositionedResult) = r.nextStringOption().map(fnFromString).getOrElse(zero)

  def updateValue(v: JSONTYPE, r: PositionedResult) = r.updateObject(mkPgObject(v))

  override def valueToSQLLiteral(v: JSONTYPE) = fnToString(v)

  ///
  private def mkPgObject(v: JSONTYPE) = {
    val obj = new PGobject
    obj.setType(sqlTypeName)
    obj.setValue(fnToString(v))
    obj
  }
}
