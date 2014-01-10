package com.github.tminglei.slickpg
package array

import scala.reflect.ClassTag
import scala.slick.lifted.{BaseTypeMapper, TypeMapperDelegate}
import scala.slick.driver.BasicProfile
import scala.slick.session.{PositionedResult, PositionedParameters}
import java.util.{Map => JMap}

class ArrayTypeMapper[T: ClassTag](sqlBaseType: String,
                                   fnFromString: (String => List[T]),
                                   fnToString: (List[T] => String))
          extends TypeMapperDelegate[List[T]] with BaseTypeMapper[List[T]] {

  def apply(v1: BasicProfile): TypeMapperDelegate[List[T]] = this

  //----------------------------------------------------------
  def zero: List[T] = Nil

  def sqlType: Int = java.sql.Types.ARRAY

  def sqlTypeName: String = s"$sqlBaseType ARRAY"

  def setValue(v: List[T], p: PositionedParameters) = p.setObject(mkArray(v), sqlType)

  def setOption(v: Option[List[T]], p: PositionedParameters) = p.setObjectOption(v.map(mkArray), sqlType)

  def nextValue(r: PositionedResult): List[T] = r.nextStringOption().map(fnFromString).getOrElse(zero)

  def updateValue(v: List[T], r: PositionedResult) = r.updateObject(mkArray(v))

  override def valueToSQLLiteral(v: List[T]) = mkArray(v).toString

  //--
  private def mkArray(v: List[T]): java.sql.Array = new SimpleArray(sqlBaseType, v, fnToString)
  
  
  /** only used to transfer array data into driver/preparedStatement */
  class SimpleArray(sqlBaseTypeName: String, vList: List[T], fnToString: (List[T] => String)) extends java.sql.Array {

    def getBaseTypeName = sqlBaseTypeName

    def getBaseType = ???

    def getArray = vList.toArray

    def getArray(map: JMap[String, Class[_]]) = ???

    def getArray(index: Long, count: Int) = ???

    def getArray(index: Long, count: Int, map: JMap[String, Class[_]]) = ???

    def getResultSet = ???

    def getResultSet(map: JMap[String, Class[_]]) = ???

    def getResultSet(index: Long, count: Int) = ???

    def getResultSet(index: Long, count: Int, map: JMap[String, Class[_]]) = ???

    def free() = { /* nothing to do */ }

    override def toString = fnToString(vList)
  }
}
