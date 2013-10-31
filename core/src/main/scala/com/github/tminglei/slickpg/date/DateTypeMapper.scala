package com.github.tminglei.slickpg
package date

import java.sql.Date
import scala.slick.lifted.{BaseTypeMapper, TypeMapperDelegate}
import scala.slick.driver.BasicProfile
import scala.slick.session.{PositionedResult, PositionedParameters}

class DateTypeMapper[DATE](fnFromDate: (Date => DATE),
                           fnToDate: (DATE => Date))
              extends TypeMapperDelegate[DATE] with BaseTypeMapper[DATE] {

  def apply(v1: BasicProfile): TypeMapperDelegate[DATE] = this

  //-----------------------------------------------------------------
  def zero: DATE = null.asInstanceOf[DATE]

  def sqlType: Int = java.sql.Types.DATE

  def sqlTypeName: String = "date"

  def setValue(v: DATE, p: PositionedParameters) = p.setDate(fnToDate(v))

  def setOption(v: Option[DATE], p: PositionedParameters) = p.setDateOption(v.map(fnToDate))

  def nextValue(r: PositionedResult): DATE = r.nextDateOption().map(fnFromDate).getOrElse(zero)

  def updateValue(v: DATE, r: PositionedResult) = r.updateDate(fnToDate(v))

  override def valueToSQLLiteral(v: DATE) = s"{d '${fnToDate(v)}'}"

}
