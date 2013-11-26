package com.github.tminglei.slickpg
package date

import java.sql.Date
import scala.slick.jdbc.{PositionedResult, PositionedParameters, JdbcType}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.reflect.ClassTag

class DateJdbcType[DATE](fnFromDate: (Date => DATE),
                         fnToDate: (DATE => Date))(
            implicit tag: ClassTag[DATE]) extends JdbcType[DATE] with BaseTypedType[DATE] {

  def scalaType: ScalaType[DATE] = ScalaBaseType[DATE]

  def zero: DATE = null.asInstanceOf[DATE]

  def sqlType: Int = java.sql.Types.DATE

  def sqlTypeName: String = "date"

  def setValue(v: DATE, p: PositionedParameters) = p.setDate(fnToDate(v))

  def setOption(v: Option[DATE], p: PositionedParameters) = p.setDateOption(v.map(fnToDate))

  def nextValue(r: PositionedResult): DATE = r.nextDateOption().map(fnFromDate).getOrElse(zero)

  def updateValue(v: DATE, r: PositionedResult) = r.updateDate(fnToDate(v))

  def hasLiteralForm: Boolean = true

  override def valueToSQLLiteral(v: DATE) = s"{d '${fnToDate(v)}'}"

}
