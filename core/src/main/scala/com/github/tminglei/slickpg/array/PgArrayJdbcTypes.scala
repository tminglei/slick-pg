package com.github.tminglei.slickpg
package array

import scala.reflect.ClassTag
import slick.ast.FieldSymbol
import slick.driver.{PostgresDriver, JdbcTypesComponent}
import slick.profile.RelationalProfile.ColumnOption.Length
import java.sql.{ResultSet, PreparedStatement}

trait PgArrayJdbcTypes extends JdbcTypesComponent { driver: PostgresDriver =>

  @deprecated(message = "use 'new SimpleArrayJdbcType[T](..).to[SEQ[T]](..)' instead", since = "0.7.1")
  class SimpleArrayListJdbcType[T](sqlBaseType: String)(
              implicit override val classTag: ClassTag[List[T]], tag: ClassTag[T])
                    extends WrappedConvArrayJdbcType[T, List](
                        new SimpleArrayJdbcType(sqlBaseType), _.toList) {

    def basedOn[U](tmap: T => U, tcomap: U => T): DriverJdbcType[List[T]] =
      delegate.asInstanceOf[SimpleArrayJdbcType[T]].basedOn(tmap, tcomap).to(_.toList)
  }

  //
  class SimpleArrayJdbcType[T](sqlBaseType: String)(
              implicit override val classTag: ClassTag[Seq[T]], tag: ClassTag[T])
                    extends DriverJdbcType[Seq[T]] {

    override def sqlType: Int = java.sql.Types.ARRAY

    override def sqlTypeName(size: Option[FieldSymbol]): String = s"$sqlBaseType ARRAY"

    override def getValue(r: ResultSet, idx: Int): Seq[T] = {
      val value = r.getArray(idx)
      if (r.wasNull) null else value.getArray.asInstanceOf[Array[Any]].map(_.asInstanceOf[T])
    }

    override def setValue(vList: Seq[T], p: PreparedStatement, idx: Int): Unit = p.setArray(idx, mkArray(vList))

    override def updateValue(vList: Seq[T], r: ResultSet, idx: Int): Unit = r.updateArray(idx, mkArray(vList))

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(vList: Seq[T]) = if(vList eq null) "NULL" else s"'${buildArrayStr(vList)}'"

    //--
    private def mkArray(v: Seq[T]): java.sql.Array = utils.SimpleArrayUtils.mkArray(buildArrayStr)(sqlBaseType, v)

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

    def to[SEQ[T] <: Seq[T]](conv: Seq[T] => SEQ[T])(implicit classTag: ClassTag[SEQ[T]]): DriverJdbcType[SEQ[T]] =
      new WrappedConvArrayJdbcType[T, SEQ](this, conv)
  }

  /* alias, added for back compatible */
  @deprecated(message = "use AdvancedArrayListJdbcType instead", since = "0.6.5")
  type NestedArrayListJdbcType[T] = AdvancedArrayListJdbcType[T]

  ///-- can be used to map complex composite/nested array
  @deprecated(message = "use 'new AdvancedArrayJdbcType[T](..).to[SEQ[T]](..)' instead", since = "0.7.1")
  class AdvancedArrayListJdbcType[T](sqlBaseType: String,
                                  fromString: (String => List[T]),
                                  mkString: (List[T] => String))(
              implicit override val classTag: ClassTag[List[T]], tag: ClassTag[T])
                    extends WrappedConvArrayJdbcType[T, List](
                        new AdvancedArrayJdbcType(sqlBaseType, fromString, v => mkString(v.toList)), _.toList)

  //
  class AdvancedArrayJdbcType[T](sqlBaseType: String,
                                fromString: (String => Seq[T]),
                                mkString: (Seq[T] => String))(
              implicit override val classTag: ClassTag[Seq[T]], tag: ClassTag[T])
                    extends DriverJdbcType[Seq[T]] {

    override def sqlType: Int = java.sql.Types.ARRAY

    override def sqlTypeName(size: Option[FieldSymbol]): String = s"$sqlBaseType ARRAY"

    override def getValue(r: ResultSet, idx: Int): Seq[T] = {
      val value = r.getString(idx)
      if (r.wasNull) null else fromString(value)
    }

    override def setValue(vList: Seq[T], p: PreparedStatement, idx: Int): Unit = p.setArray(idx, mkArray(vList))

    override def updateValue(vList: Seq[T], r: ResultSet, idx: Int): Unit = r.updateArray(idx, mkArray(vList))

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(vList: Seq[T]) = if(vList eq null) "NULL" else s"'${mkString(vList)}'"

    //--
    private def mkArray(v: Seq[T]): java.sql.Array = utils.SimpleArrayUtils.mkArray(mkString)(sqlBaseType, v)

    def to[SEQ[T] <: Seq[T]](conv: Seq[T] => SEQ[T])(implicit classTag: ClassTag[SEQ[T]]): DriverJdbcType[SEQ[T]] =
      new WrappedConvArrayJdbcType[T, SEQ](this, conv)
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  private[array] class WrappedConvArrayJdbcType[T, SEQ[T] <: Seq[T]](val delegate: DriverJdbcType[Seq[T]], val conv: Seq[T] => SEQ[T])(
      implicit override val classTag: ClassTag[SEQ[T]], tag: ClassTag[T]) extends DriverJdbcType[SEQ[T]] {

    override def sqlType: Int = delegate.sqlType

    override def sqlTypeName(size: Option[FieldSymbol]): String = delegate.sqlTypeName(size)

    override def getValue(r: ResultSet, idx: Int): SEQ[T] = Option(delegate.getValue(r, idx)).map(conv).getOrElse(null.asInstanceOf[SEQ[T]])

    override def setValue(vList: SEQ[T], p: PreparedStatement, idx: Int): Unit = delegate.setValue(vList, p, idx)

    override def updateValue(vList: SEQ[T], r: ResultSet, idx: Int): Unit = delegate.updateValue(vList, r, idx)

    override def hasLiteralForm: Boolean = delegate.hasLiteralForm

    override def valueToSQLLiteral(vList: SEQ[T]) = delegate.valueToSQLLiteral(Option(vList).orNull)
  }
}
