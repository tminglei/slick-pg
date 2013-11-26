package com.github.tminglei.slickpg
package hstore

import scala.collection.convert.{WrapAsJava, WrapAsScala}
import org.postgresql.util.{PGobject, HStoreConverter}
import scala.slick.jdbc.{PositionedResult, PositionedParameters, JdbcType}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.reflect.ClassTag

class HStoreMapJdbcType(implicit tag: ClassTag[Map[String, String]])
            extends JdbcType[Map[String, String]] with BaseTypedType[Map[String, String]] {

  def scalaType: ScalaType[Map[String, String]] = ScalaBaseType[Map[String, String]]

  def zero = Map.empty[String, String]

  def sqlType = java.sql.Types.OTHER

  def sqlTypeName = "hstore"

  def setValue(v: Map[String, String], p: PositionedParameters) = p.setObject(mkPgObject(v), sqlType)

  def setOption(v: Option[Map[String, String]], p: PositionedParameters) = p.setObjectOption(v.map(mkPgObject), sqlType)

  def nextValue(r: PositionedResult) = {
    r.nextObjectOption().map(_.asInstanceOf[java.util.Map[String, String]])
      .map(WrapAsScala.mapAsScalaMap(_).toMap)
      .getOrElse(zero)
  }

  def updateValue(v: Map[String, String], r: PositionedResult) = r.updateObject(WrapAsJava.mapAsJavaMap(v))

  def hasLiteralForm: Boolean = true

  override def valueToSQLLiteral(v: Map[String, String]) = HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v))

  ///
  private def mkPgObject(v: Map[String, String]) = {
    val obj = new PGobject
    obj.setType(sqlTypeName)
    obj.setValue(valueToSQLLiteral(v))
    obj
  }
}
