package com.github.tminglei.slickpg.hstore

import scala.slick.lifted.{BaseTypeMapper, TypeMapperDelegate}
import scala.slick.driver.BasicProfile
import scala.slick.session.{PositionedResult, PositionedParameters}
import scala.collection.convert.{WrapAsJava, WrapAsScala}
import org.postgresql.util.{PGobject, HStoreConverter}

class HStoreTypeMapper extends TypeMapperDelegate[Map[String, String]] with BaseTypeMapper[Map[String, String]] {

  def apply(v1: BasicProfile): TypeMapperDelegate[Map[String, String]] = this

  //----------------------------------------------------------
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

  override def valueToSQLLiteral(v: Map[String, String]) = HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v))

  ///
  private def mkPgObject(v: Map[String, String]) = {
    val obj = new PGobject
    obj.setType(sqlTypeName)
    obj.setValue(valueToSQLLiteral(v))
    obj
  }
}
