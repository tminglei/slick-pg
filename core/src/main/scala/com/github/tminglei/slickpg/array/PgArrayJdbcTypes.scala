package com.github.tminglei.slickpg
package array

import scala.reflect.ClassTag
import java.util.{Map => JMap}
import scala.slick.driver.{PostgresDriver, JdbcTypesComponent}
import java.sql.{ResultSet, PreparedStatement}

trait PgArrayJdbcTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  class SimpleArrayListJdbcType[T](sqlBaseType: String)(
              implicit override val classTag: ClassTag[List[T]], tag: ClassTag[T])
                    extends WrappedConvArrayJdbcType[T, List](
                        new SimpleArrayJdbcType(sqlBaseType), _.toList, identity) {

    def basedOn[U](tmap: T => U, tcomap: U => T): DriverJdbcType[List[T]] =
      delegate.asInstanceOf[SimpleArrayJdbcType[T]].basedOn(tmap, tcomap).to(_.toList, identity)
  }

  //
  class SimpleArrayJdbcType[T](sqlBaseType: String)(
              implicit override val classTag: ClassTag[Seq[T]], tag: ClassTag[T])
                    extends DriverJdbcType[Seq[T]] {

    override def sqlType: Int = java.sql.Types.ARRAY

    override def sqlTypeName: String = s"$sqlBaseType ARRAY"

    override def getValue(r: ResultSet, idx: Int): Seq[T] = {
      val value = r.getArray(idx)
      if (r.wasNull) null else value.getArray.asInstanceOf[Array[Any]].map(_.asInstanceOf[T])
    }

    override def setValue(vList: Seq[T], p: PreparedStatement, idx: Int): Unit = p.setArray(idx, mkArray(vList))

    override def updateValue(vList: Seq[T], r: ResultSet, idx: Int): Unit = r.updateArray(idx, mkArray(vList))

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(vList: Seq[T]) = if(vList eq null) "NULL" else s"'${buildArrayStr(vList)}'"

    //--
    private def mkArray(v: Seq[T]): java.sql.Array = new SimpleArray(sqlBaseType, v, buildArrayStr)

    protected def buildArrayStr(vList: Seq[Any]): String = utils.SimpleArrayUtils.mkString[Any](_.toString)(vList)

    ///
    def basedOn[U](tmap: T => U, tcomap: U => T): SimpleArrayJdbcType[T] =
      new SimpleArrayJdbcType[T](sqlBaseType) {

        override def getValue(r: ResultSet, idx: Int): Seq[T] = {
          val value = r.getArray(idx)
          if (r.wasNull) null else value.getArray.asInstanceOf[Array[Any]]
            .map(e => tcomap(e.asInstanceOf[U]))
        }

        //--
        override protected def buildArrayStr(v: Seq[Any]): String = super.buildArrayStr(v.map(e => tmap(e.asInstanceOf[T])))
      }

    def to[SEQ[T]](conv: Seq[T] => SEQ[T], rconv: SEQ[T] => Seq[T])(implicit classTag: ClassTag[SEQ[T]]): DriverJdbcType[SEQ[T]] =
      new WrappedConvArrayJdbcType[T, SEQ](this, conv, rconv)
  }

  /* alias, added for back compatible */
  @deprecated(message = "use AdvancedArrayListJdbcType instead", since = "0.6.5")
  type NestedArrayListJdbcType[T] = AdvancedArrayListJdbcType[T]

  ///-- can be used to map complex composite/nested array
  class AdvancedArrayListJdbcType[T](sqlBaseType: String,
                                  fromString: (String => List[T]),
                                  mkString: (List[T] => String))(
              implicit override val classTag: ClassTag[List[T]], tag: ClassTag[T])
                    extends WrappedConvArrayJdbcType[T, List](
                        new AdvancedArrayJdbcType(sqlBaseType, fromString, v => mkString(v.toList)), _.toList, identity)

  //
  class AdvancedArrayJdbcType[T](sqlBaseType: String,
                                fromString: (String => Seq[T]),
                                mkString: (Seq[T] => String))(
              implicit override val classTag: ClassTag[Seq[T]], tag: ClassTag[T])
                    extends DriverJdbcType[Seq[T]] {

    override def sqlType: Int = java.sql.Types.ARRAY

    override def sqlTypeName: String = s"$sqlBaseType ARRAY"

    override def getValue(r: ResultSet, idx: Int): Seq[T] = {
      val value = r.getString(idx)
      if (r.wasNull) null else fromString(value)
    }

    override def setValue(vList: Seq[T], p: PreparedStatement, idx: Int): Unit = p.setArray(idx, mkArray(vList))

    override def updateValue(vList: Seq[T], r: ResultSet, idx: Int): Unit = r.updateArray(idx, mkArray(vList))

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(vList: Seq[T]) = if(vList eq null) "NULL" else s"'${mkString(vList)}'"

    //--
    private def mkArray(v: Seq[T]): java.sql.Array = new SimpleArray(sqlBaseType, v, mkString)

    def to[SEQ[T]](conv: Seq[T] => SEQ[T], rconv: SEQ[T] => Seq[T])(implicit classTag: ClassTag[SEQ[T]]): DriverJdbcType[SEQ[T]] =
      new WrappedConvArrayJdbcType[T, SEQ](this, conv, rconv)
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  private[array] class WrappedConvArrayJdbcType[T, SEQ[T]](val delegate: DriverJdbcType[Seq[T]],
                        val conv: Seq[T] => SEQ[T], val rconv: SEQ[T] => Seq[T])(
      implicit override val classTag: ClassTag[SEQ[T]], tag: ClassTag[T]) extends DriverJdbcType[SEQ[T]] {

    override def sqlType: Int = delegate.sqlType

    override def sqlTypeName: String = delegate.sqlTypeName

    override def getValue(r: ResultSet, idx: Int): SEQ[T] = Option(delegate.getValue(r, idx)).map(conv).getOrElse(null.asInstanceOf[SEQ[T]])

    override def setValue(vList: SEQ[T], p: PreparedStatement, idx: Int): Unit = delegate.setValue(rconv(vList), p, idx)

    override def updateValue(vList: SEQ[T], r: ResultSet, idx: Int): Unit = delegate.updateValue(rconv(vList), r, idx)

    override def hasLiteralForm: Boolean = delegate.hasLiteralForm

    override def valueToSQLLiteral(vList: SEQ[T]) = delegate.valueToSQLLiteral(Option(vList).map(rconv).orNull)
  }

  /** only used to transfer array data into driver/preparedStatement */
  private class SimpleArray[T : ClassTag](sqlBaseTypeName: String, vList: Seq[T], mkString: (Seq[T] => String)) extends java.sql.Array {

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
