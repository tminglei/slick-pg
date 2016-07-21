package com.github.tminglei.slickpg
package array

import scala.reflect.ClassTag
import slick.ast.FieldSymbol
import java.sql.{PreparedStatement, ResultSet}

import slick.jdbc.{JdbcTypesComponent, PostgresProfile}

trait PgArrayJdbcTypes extends JdbcTypesComponent { driver: PostgresProfile =>

  class SimpleArrayJdbcType[T] private[slickpg] (sqlBaseType: String,
                                                tmap: Any => T,
                                                tcomap: T => Any,
                                                zero: Seq[T] = null.asInstanceOf[Seq[T]])(
              implicit override val classTag: ClassTag[Seq[T]], ctag: ClassTag[T], checked: ElemWitness[T])
                    extends DriverJdbcType[Seq[T]] { self =>

    def this(sqlBaseType: String)(implicit ctag: ClassTag[T], checked: ElemWitness[T]) = this(sqlBaseType, _.asInstanceOf[T], identity)

    override def sqlType: Int = java.sql.Types.ARRAY

    override def sqlTypeName(size: Option[FieldSymbol]): String = s"$sqlBaseType ARRAY"

    override def getValue(r: ResultSet, idx: Int): Seq[T] = {
      val value = r.getArray(idx)
      if (r.wasNull) zero else value.getArray.asInstanceOf[Array[Any]].map(tmap)
    }

    override def setValue(vList: Seq[T], p: PreparedStatement, idx: Int): Unit = p.setArray(idx, mkArray(vList))

    override def updateValue(vList: Seq[T], r: ResultSet, idx: Int): Unit = r.updateArray(idx, mkArray(vList))

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(vList: Seq[T]) = if(vList eq null) "NULL" else s"'${buildArrayStr(vList)}'"

    //--
    private def mkArray(v: Seq[T]): java.sql.Array = utils.SimpleArrayUtils.mkArray(buildArrayStr)(sqlBaseType, v.map(tcomap))

    protected def buildArrayStr(vList: Seq[Any]): String = utils.SimpleArrayUtils.mkString[Any](_.toString)(vList)

    ///
    def mapTo[U](tmap: T => U, tcomap: U => T)(implicit ctags: ClassTag[Seq[U]], ctag: ClassTag[U]): SimpleArrayJdbcType[U] =
      new SimpleArrayJdbcType[U](sqlBaseType, v => tmap(self.tmap(v)), r => self.tcomap(tcomap(r)))(ctags, ctag, ElemWitness.AnyWitness.asInstanceOf[ElemWitness[U]])

    def to[SEQ[T] <: Seq[T]](conv: Seq[T] => SEQ[T])(implicit classTag: ClassTag[SEQ[T]]): DriverJdbcType[SEQ[T]] =
      new WrappedConvArrayJdbcType[T, SEQ](this, conv)
  }

  ///
  class AdvancedArrayJdbcType[T](sqlBaseType: String,
                                fromString: (String => Seq[T]),
                                mkString: (Seq[T] => String),
                                zero: Seq[T] = null.asInstanceOf[Seq[T]])(
              implicit override val classTag: ClassTag[Seq[T]], tag: ClassTag[T])
                    extends DriverJdbcType[Seq[T]] {

    override def sqlType: Int = java.sql.Types.ARRAY

    override def sqlTypeName(size: Option[FieldSymbol]): String = s"$sqlBaseType ARRAY"

    override def getValue(r: ResultSet, idx: Int): Seq[T] = {
      val value = r.getString(idx)
      if (r.wasNull) zero else fromString(value)
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
      implicit override val classTag: ClassTag[SEQ[T]], ctag: ClassTag[T]) extends DriverJdbcType[SEQ[T]] {

    override def sqlType: Int = delegate.sqlType

    override def sqlTypeName(size: Option[FieldSymbol]): String = delegate.sqlTypeName(size)

    override def getValue(r: ResultSet, idx: Int): SEQ[T] = Option(delegate.getValue(r, idx)).map(conv).getOrElse(null.asInstanceOf[SEQ[T]])

    override def setValue(vList: SEQ[T], p: PreparedStatement, idx: Int): Unit = delegate.setValue(vList, p, idx)

    override def updateValue(vList: SEQ[T], r: ResultSet, idx: Int): Unit = delegate.updateValue(vList, r, idx)

    override def hasLiteralForm: Boolean = delegate.hasLiteralForm

    override def valueToSQLLiteral(vList: SEQ[T]) = delegate.valueToSQLLiteral(Option(vList).orNull)
  }

  /// added to help check built-in support array types statically
  sealed trait ElemWitness[T]

  object ElemWitness {
    implicit object LongWitness extends ElemWitness[Long]
    implicit object IntWitness extends ElemWitness[Int]
    implicit object ShortWitness extends ElemWitness[Short]
    implicit object FloatWitness extends ElemWitness[Float]
    implicit object DoubleWitness extends ElemWitness[Double]
    implicit object BooleanWitness extends ElemWitness[Boolean]
    implicit object StringWitness extends ElemWitness[String]
    implicit object UUIDWitness extends ElemWitness[java.util.UUID]
    implicit object DateWitness extends ElemWitness[java.sql.Date]
    implicit object TimeWitness extends ElemWitness[java.sql.Time]
    implicit object TimestampWitness extends ElemWitness[java.sql.Timestamp]
    implicit object BigDecimalWitness extends ElemWitness[java.math.BigDecimal]

    object AnyWitness extends ElemWitness[Nothing]
  }
}
