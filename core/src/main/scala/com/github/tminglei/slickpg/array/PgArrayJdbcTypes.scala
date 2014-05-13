package com.github.tminglei.slickpg
package array

import scala.reflect.ClassTag
import java.util.{Map => JMap}
import scala.slick.driver.{PostgresDriver, JdbcTypesComponent}
import java.sql.{ResultSet, PreparedStatement}

trait PgArrayJdbcTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  class SimpleArrayListJdbcType[T](sqlBaseType: String)(
              implicit override val classTag: ClassTag[List[T]], tag: ClassTag[T])
                    extends DriverJdbcType[List[T]] {

    override def sqlType: Int = java.sql.Types.ARRAY

    override def sqlTypeName: String = s"$sqlBaseType ARRAY"

    override def getValue(r: ResultSet, idx: Int): List[T] = {
      val value = r.getArray(idx)
      if (r.wasNull) null else value.getArray.asInstanceOf[Array[Any]].map(_.asInstanceOf[T]).toList
    }

    override def setValue(vList: List[T], p: PreparedStatement, idx: Int): Unit = p.setArray(idx, mkArray(vList))

    override def updateValue(vList: List[T], r: ResultSet, idx: Int): Unit = r.updateArray(idx, mkArray(vList))

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(vList: List[T]) = if(vList eq null) "NULL" else s"'${buildArrayStr(vList)}'"

    ///
    def basedOn[U](tmap: T => U, tcomap: U => T): DriverJdbcType[List[T]] =
      new SimpleArrayListJdbcType[T](sqlBaseType) {

        override def getValue(r: ResultSet, idx: Int): List[T] = {
          val value = r.getArray(idx)
          if (r.wasNull) null else value.getArray.asInstanceOf[Array[Any]]
            .map(e => tcomap(e.asInstanceOf[U])).toList
        }

        //--
        override protected def buildArrayStr(v: List[Any]): String = super.buildArrayStr(v.map(e => tmap(e.asInstanceOf[T])))
      }

    //--
    private def mkArray(v: List[T]): java.sql.Array = new SimpleArray(sqlBaseType, v, buildArrayStr)

    /** copy from [[org.postgresql.jdbc4.AbstractJdbc4Connection#createArrayOf(..)]]
      * and [[org.postgresql.jdbc2.AbstractJdbc2Array#escapeArrayElement(..)]] */
    protected def buildArrayStr(vList: List[Any]): String = {
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
  class NestedArrayListJdbcType[T](sqlBaseType: String,
                            fnFromString: (String => List[T]),
                            fnToString: (List[T] => String))(
              implicit override val classTag: ClassTag[List[T]], tag: ClassTag[T])
                    extends DriverJdbcType[List[T]] {

    override def sqlType: Int = java.sql.Types.ARRAY

    override def sqlTypeName: String = s"$sqlBaseType ARRAY"

    override def getValue(r: ResultSet, idx: Int): List[T] = {
      val value = r.getString(idx)
      if (r.wasNull) null else fnFromString(value)
    }

    override def setValue(vList: List[T], p: PreparedStatement, idx: Int): Unit = p.setArray(idx, mkArray(vList))

    override def updateValue(vList: List[T], r: ResultSet, idx: Int): Unit = r.updateArray(idx, mkArray(vList))

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(vList: List[T]) = if(vList eq null) "NULL" else s"'${fnToString(vList)}'"

    //--
    private def mkArray(v: List[T]): java.sql.Array = new SimpleArray(sqlBaseType, v, fnToString)
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
