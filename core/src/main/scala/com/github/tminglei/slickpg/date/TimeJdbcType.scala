package com.github.tminglei.slickpg
package date

import java.sql.Time
import scala.slick.jdbc.{PositionedResult, PositionedParameters, JdbcType}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.reflect.ClassTag

class TimeJdbcType[TIME](fnFromTime: (Time => TIME),
                         fnToTime: (TIME => Time))(
            implicit tag: ClassTag[TIME]) extends JdbcType[TIME] with BaseTypedType[TIME] {

  def scalaType: ScalaType[TIME] = ScalaBaseType[TIME]

  def zero: TIME = null.asInstanceOf[TIME]

  def sqlType: Int = java.sql.Types.TIME

  def sqlTypeName: String = "time"

  def setValue(v: TIME, p: PositionedParameters) = p.setTime(fnToTime(v))

  def setOption(v: Option[TIME], p: PositionedParameters) = p.setTimeOption(v.map(fnToTime))

  def nextValue(r: PositionedResult): TIME = r.nextTimeOption().map(fnFromTime).getOrElse(zero)

  def updateValue(v: TIME, r: PositionedResult) = r.updateTime(fnToTime(v))

  def hasLiteralForm: Boolean = true

  override def valueToSQLLiteral(v: TIME) = s"{t '${fnToTime(v)}'}"
  
}
