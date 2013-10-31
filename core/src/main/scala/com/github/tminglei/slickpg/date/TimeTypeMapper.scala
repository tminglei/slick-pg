package com.github.tminglei.slickpg
package date

import java.sql.Time
import scala.slick.lifted.{BaseTypeMapper, TypeMapperDelegate}
import scala.slick.driver.BasicProfile
import scala.slick.session.{PositionedResult, PositionedParameters}

class TimeTypeMapper[TIME](fnFromTime: (Time => TIME),
                           fnToTime: (TIME => Time))
              extends TypeMapperDelegate[TIME] with BaseTypeMapper[TIME] {

  def apply(v1: BasicProfile): TypeMapperDelegate[TIME] = this

  //-----------------------------------------------------------------
  def zero: TIME = null.asInstanceOf[TIME]

  def sqlType: Int = java.sql.Types.TIME

  def sqlTypeName: String = "time"

  def setValue(v: TIME, p: PositionedParameters) = p.setTime(fnToTime(v))

  def setOption(v: Option[TIME], p: PositionedParameters) = p.setTimeOption(v.map(fnToTime))

  def nextValue(r: PositionedResult): TIME = r.nextTimeOption().map(fnFromTime).getOrElse(zero)

  def updateValue(v: TIME, r: PositionedResult) = r.updateTime(fnToTime(v))

  override def valueToSQLLiteral(v: TIME) = s"{t '${fnToTime(v)}'}"

}
