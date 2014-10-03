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

    protected def buildArrayStr(vList: List[Any]): String = utils.SimpleArrayUtils.mkString[Any](_.toString)(vList)
  }

  ///-- can be used to map complex composite/nested array
  @deprecated /* alias, added for back compatible */
  type NestedArrayListJdbcType[T] = AdvancedArrayListJdbcType[T]

  class AdvancedArrayListJdbcType[T](sqlBaseType: String,
                                  fromString: (String => List[T]),
                                  mkString: (List[T] => String))(
              implicit override val classTag: ClassTag[List[T]], tag: ClassTag[T])
                    extends DriverJdbcType[List[T]] {

    override def sqlType: Int = java.sql.Types.ARRAY

    override def sqlTypeName: String = s"$sqlBaseType ARRAY"

    override def getValue(r: ResultSet, idx: Int): List[T] = {
      val value = r.getString(idx)
      if (r.wasNull) null else fromString(value)
    }

    override def setValue(vList: List[T], p: PreparedStatement, idx: Int): Unit = p.setArray(idx, mkArray(vList))

    override def updateValue(vList: List[T], r: ResultSet, idx: Int): Unit = r.updateArray(idx, mkArray(vList))

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(vList: List[T]) = if(vList eq null) "NULL" else s"'${mkString(vList)}'"

    //--
    private def mkArray(v: List[T]): java.sql.Array = new SimpleArray(sqlBaseType, v, mkString)
  }

  /** only used to transfer array data into driver/preparedStatement */
  private class SimpleArray[T : ClassTag](sqlBaseTypeName: String, vList: List[T], mkString: (List[T] => String)) extends java.sql.Array {

    def getBaseTypeName = sqlBaseTypeName.replaceFirst("^\"", "").replaceFirst("\"$", "")

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

    override def toString = mkString(vList)
  }
}
