package com.github.tminglei.slickpg
package array

import scala.reflect.ClassTag
import java.util.{Map => JMap}
import scala.slick.jdbc.{PositionedResult, PositionedParameters}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.slick.driver.{PostgresDriver, JdbcTypesComponent}

trait PgArrayJavaTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  class ArrayListJavaType[T: ClassTag](sqlBaseType: String,
                                       fnFromString: (String => List[T]),
                                       fnToString: (List[T] => String))
                           extends JdbcType[List[T]] with BaseTypedType[List[T]] {

    def scalaType: ScalaType[List[T]] = ScalaBaseType[List[T]]

    def zero: List[T] = Nil

    def sqlType: Int = java.sql.Types.ARRAY

    def sqlTypeName: String = s"$sqlBaseType ARRAY"

    def setValue(v: List[T], p: PositionedParameters) = p.setObject(mkArray(v), sqlType)

    def setOption(v: Option[List[T]], p: PositionedParameters) = p.setObjectOption(v.map(mkArray), sqlType)

    def nextValue(r: PositionedResult): List[T] = r.nextStringOption().map(fnFromString).getOrElse(zero)

    def updateValue(v: List[T], r: PositionedResult) = r.updateObject(mkArray(v))

    def hasLiteralForm: Boolean = false

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
}
