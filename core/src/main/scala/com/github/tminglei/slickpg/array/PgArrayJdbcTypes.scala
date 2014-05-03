package com.github.tminglei.slickpg
package array

import scala.reflect.ClassTag
import java.util.{Map => JMap}
import scala.slick.jdbc.{PositionedResult, PositionedParameters}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.slick.driver.{PostgresDriver, JdbcTypesComponent}

trait PgArrayJdbcTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  class SimpleArrayListJdbcType[T : ClassTag](sqlBaseType: String) extends JdbcType[List[T]] with BaseTypedType[List[T]] {

    def scalaType: ScalaType[List[T]] = ScalaBaseType[List[T]]

    def zero: List[T] = Nil

    def sqlType: Int = java.sql.Types.ARRAY

    def sqlTypeName: String = s"$sqlBaseType ARRAY"

    def setValue(vList: List[T], p: PositionedParameters) = p.setObject(mkArray(vList), sqlType)

    def setOption(vList: Option[List[T]], p: PositionedParameters) = p.setObjectOption(vList.map(mkArray), sqlType)

    def nextValue(r: PositionedResult): List[T] =
      r.nextObjectOption().map(_.asInstanceOf[java.sql.Array])
        .map(_.getArray.asInstanceOf[Array[Any]].map(_.asInstanceOf[T]).toList)
        .getOrElse(zero)

    def updateValue(vList: List[T], r: PositionedResult) = r.updateObject(mkArray(vList))

    def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(vList: List[T]) = buildArrayStr(vList)

    ///
    def basedOn[U](tmap: T => U, tcomap: U => T): JdbcType[List[T]] with BaseTypedType[List[T]] =
      new SimpleArrayListJdbcType[T](sqlBaseType) {

        override def nextValue(r: PositionedResult): List[T] =
          r.nextObjectOption().map(_.asInstanceOf[java.sql.Array])
            .map(_.getArray.asInstanceOf[Array[Any]].map(e => tcomap(e.asInstanceOf[U])).toList)
            .getOrElse(zero)

        //--
        override protected def buildArrayStr[E](v: List[E]): String = super.buildArrayStr(v.map(e => tmap(e.asInstanceOf[T])))
      }

    //--
    private def mkArray(v: List[T]): java.sql.Array = new SimpleArray(sqlBaseType, v, buildArrayStr)

    /** copy from [[org.postgresql.jdbc4.AbstractJdbc4Connection#createArrayOf(..)]]
      * and [[org.postgresql.jdbc2.AbstractJdbc2Array#escapeArrayElement(..)]] */
    protected def buildArrayStr[E](vList: List[E]): String = {
      def escape(s: String) = {
        StringBuilder.newBuilder + '"' appendAll (
          s map {
            c => if (c == '"' || c == '\\') '\\' else c
          }) + '"'
      }

      StringBuilder.newBuilder + '{' append (
        vList map {
          case v: Any => escape(v.toString) // use 'v: Any' instead of 'v: T' (jvm type erase)
          case _ => "NULL"
        } mkString(",")
      ) + '}' toString
    }
  }

  ///-- can be used to map complex composite/nested array
  class ArrayListJdbcType[T : ClassTag](sqlBaseType: String,
                                       fnFromString: (String => List[T]),
                                       fnToString: (List[T] => String))
                           extends JdbcType[List[T]] with BaseTypedType[List[T]] {

    def scalaType: ScalaType[List[T]] = ScalaBaseType[List[T]]

    def zero: List[T] = Nil

    def sqlType: Int = java.sql.Types.ARRAY

    def sqlTypeName: String = s"$sqlBaseType ARRAY"

    def setValue(vList: List[T], p: PositionedParameters) = p.setObject(mkArray(vList), sqlType)

    def setOption(vList: Option[List[T]], p: PositionedParameters) = p.setObjectOption(vList.map(mkArray), sqlType)

    def nextValue(r: PositionedResult): List[T] = r.nextStringOption().map(fnFromString).getOrElse(zero)

    def updateValue(vList: List[T], r: PositionedResult) = r.updateObject(mkArray(vList))

    def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(vList: List[T]) = fnToString(vList)

    //--
    private def mkArray(vList: List[T]): java.sql.Array = new SimpleArray(sqlBaseType, vList, fnToString)
  }

  /** only used to transfer array data into driver/preparedStatement */
  private class SimpleArray[T : ClassTag](sqlBaseTypeName: String, vList: List[T], fnToString: (List[T] => String)) extends java.sql.Array {

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
