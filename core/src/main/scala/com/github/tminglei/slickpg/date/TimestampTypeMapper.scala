package com.github.tminglei.slickpg.date

import java.sql.Timestamp
import scala.slick.lifted.{BaseTypeMapper, TypeMapperDelegate}
import scala.slick.driver.BasicProfile
import scala.slick.session.{PositionedResult, PositionedParameters}

class TimestampTypeMapper[TIMESTAMP](fnFromTimestamp: (Timestamp => TIMESTAMP),
                                     fnToTimestamp: (TIMESTAMP => Timestamp))
            extends TypeMapperDelegate[TIMESTAMP] with BaseTypeMapper[TIMESTAMP] {

  def apply(v1: BasicProfile): TypeMapperDelegate[TIMESTAMP] = this

  //-----------------------------------------------------------------
  def zero: TIMESTAMP = null.asInstanceOf[TIMESTAMP]

  def sqlType: Int = java.sql.Types.TIMESTAMP

  def sqlTypeName: String = "timestamp"

  def setValue(v: TIMESTAMP, p: PositionedParameters) = p.setTimestamp(fnToTimestamp(v))

  def setOption(v: Option[TIMESTAMP], p: PositionedParameters) = p.setTimestampOption(v.map(fnToTimestamp))

  def nextValue(r: PositionedResult): TIMESTAMP = r.nextTimestampOption().map(fnFromTimestamp).getOrElse(zero)

  def updateValue(v: TIMESTAMP, r: PositionedResult) = r.updateTimestamp(fnToTimestamp(v))

  override def valueToSQLLiteral(v: TIMESTAMP) = s"{ts '${fnToTimestamp(v)}'}"

}
