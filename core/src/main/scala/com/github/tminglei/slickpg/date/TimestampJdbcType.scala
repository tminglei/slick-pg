package com.github.tminglei.slickpg
package date

import java.sql.Timestamp
import scala.slick.jdbc.{PositionedResult, PositionedParameters, JdbcType}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.reflect.ClassTag

class TimestampJdbcType[TIMESTAMP](fnFromTimestamp: (Timestamp => TIMESTAMP),
                                   fnToTimestamp: (TIMESTAMP => Timestamp))(
            implicit tag: ClassTag[TIMESTAMP]) extends JdbcType[TIMESTAMP] with BaseTypedType[TIMESTAMP] {

  def scalaType: ScalaType[TIMESTAMP] = ScalaBaseType[TIMESTAMP]

  def zero: TIMESTAMP = null.asInstanceOf[TIMESTAMP]

  def sqlType: Int = java.sql.Types.TIMESTAMP

  def sqlTypeName: String = "timestamp"

  def setValue(v: TIMESTAMP, p: PositionedParameters) = p.setTimestamp(fnToTimestamp(v))

  def setOption(v: Option[TIMESTAMP], p: PositionedParameters) = p.setTimestampOption(v.map(fnToTimestamp))

  def nextValue(r: PositionedResult): TIMESTAMP = r.nextTimestampOption().map(fnFromTimestamp).getOrElse(zero)

  def updateValue(v: TIMESTAMP, r: PositionedResult) = r.updateTimestamp(fnToTimestamp(v))

  def hasLiteralForm: Boolean = true

  override def valueToSQLLiteral(v: TIMESTAMP) = s"{ts '${fnToTimestamp(v)}'}"

}
