package com.github.tminglei.slickpg
package array

import scala.reflect.ClassTag
import java.util.{Map => JMap}
import scala.slick.jdbc.{JdbcType, PositionedResult, PositionedParameters}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}

class ArrayListJavaType[T: ClassTag](pgBaseType: String) extends JdbcType[List[T]] with BaseTypedType[List[T]] {

  def scalaType: ScalaType[List[T]] = ScalaBaseType[List[T]]

  def zero: List[T] = Nil

  def sqlType: Int = java.sql.Types.ARRAY

  def sqlTypeName: String = s"$pgBaseType ARRAY"

  def setValue(v: List[T], p: PositionedParameters) = p.setObject(mkArray(v), sqlType)

  def setOption(v: Option[List[T]], p: PositionedParameters) = p.setObjectOption(v.map(mkArray), sqlType)

  def nextValue(r: PositionedResult): List[T] = {
    r.nextObjectOption().map(_.asInstanceOf[java.sql.Array])
      .map(_.getArray.asInstanceOf[Array[Any]].map(_.asInstanceOf[T]).toList)
      .getOrElse(zero)
  }

  def updateValue(v: List[T], r: PositionedResult) = r.updateObject(mkArray(v))

  def hasLiteralForm: Boolean = true

  override def valueToSQLLiteral(v: List[T]) = mkArray(v).toString

  //--
  private def mkArray(v: List[T]): java.sql.Array = new SimpleArray(v, pgBaseType)
  
  
  /** only used to transfer array data into driver/preparedStatement */
  class SimpleArray(arr: Seq[Any], baseTypeName: String) extends java.sql.Array {

    def getBaseTypeName = baseTypeName

    def getBaseType = ???

    def getArray = arr.toArray

    def getArray(map: JMap[String, Class[_]]) = ???

    def getArray(index: Long, count: Int) = ???

    def getArray(index: Long, count: Int, map: JMap[String, Class[_]]) = ???

    def getResultSet = ???

    def getResultSet(map: JMap[String, Class[_]]) = ???

    def getResultSet(index: Long, count: Int) = ???

    def getResultSet(index: Long, count: Int, map: JMap[String, Class[_]]) = ???

    override def toString = buildStr(arr).toString()

    def free() = { /* nothing to do */ }

    /** copy from [[org.postgresql.jdbc4.AbstractJdbc4Connection#createArrayOf(..)]]
      * and [[org.postgresql.jdbc2.AbstractJdbc2Array#escapeArrayElement(..)]] */
    private def buildStr(elements: Seq[Any]): StringBuilder = {
      def escape(s: String) = {
        StringBuilder.newBuilder + '"' appendAll (
          s map {
            c => if (c == '"' || c == '\\') '\\' else c
          }) + '"'
      }

      StringBuilder.newBuilder + '{' append (
        elements map {
          case arr: Seq[Any] => buildStr(arr)
          case o: Any if (o != null) => escape(o.toString)
          case _ => "NULL"
        } mkString(",")) + '}'
    }
  }
}
