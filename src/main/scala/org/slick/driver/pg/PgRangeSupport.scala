package org.slick.driver
package pg

import org.slick.Range
import java.sql.{Date, Timestamp}
import scala.slick.driver.{BasicProfile, PostgresDriver}
import scala.slick.lifted._
import scala.slick.ast.Library.SqlOperator
import scala.slick.ast.{Library, Node}
import scala.slick.session.{PositionedResult, PositionedParameters}
import org.postgresql.util.PGobject

trait PgRangeSupport { driver: PostgresDriver =>

  private val tsFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  private val dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd")
  private def toTimestamp(str: String) = new Timestamp(tsFormatter.parse(str).getTime)
  private def toSQLDate(str: String) = new Date(dateFormatter.parse(str).getTime)

  trait RangeImplicits {
    implicit val intRangeTypeMapper = new RangeTypeMapper[Int]("int4range", _.toInt)
    implicit val longRangeTypeMapper = new RangeTypeMapper[Long]("int8range", _.toLong)
    implicit val floatRangeTypeMapper = new RangeTypeMapper[Float]("numrange", _.toFloat)
    implicit val timestampRangeTypeMapper = new RangeTypeMapper[Timestamp]("tsrange", toTimestamp)
    implicit val dateRangeTypeMapper = new RangeTypeMapper[Date]("daterange", toSQLDate)

    implicit def rangeColumnExtensionMethods[B0](c: Column[Range[B0]])(
      implicit tm: TypeMapper[B0], tm1: RangeTypeMapper[B0]) = {
        new RangeColumnExtensionMethods[B0, Range[B0]](c)
      }
    implicit def rangeOptionColumnExtensionMethods[B0](c: Column[Option[Range[B0]]])(
      implicit tm: TypeMapper[B0], tm1: RangeTypeMapper[B0]) = {
        new RangeColumnExtensionMethods[B0, Option[Range[B0]]](c)
      }
  }

  ////////////////////////////////////////////////////////////////////////////////

  object RangeLibrary {
    val Contains = new SqlOperator("@>")
    val ContainedBy = new SqlOperator("<@")
    val Overlap = new SqlOperator("&&")
    val StrictLeft = new SqlOperator("<<")
    val StrictRight = new SqlOperator(">>")
    val NotExtendRight = new SqlOperator("&<")
    val NotExtendLeft = new SqlOperator("&>")
    val Adjacent = new SqlOperator("-|-")

    val Union = new SqlOperator("+")
    val Intersection = new SqlOperator("*")
    val Subtraction = new SqlOperator("-")
  }

  class RangeColumnExtensionMethods[B0, P1](val c: Column[P1])(
              implicit tm: TypeMapper[B0], tm1: TypeMapper[Range[B0]]) extends ExtensionMethods[Range[B0], P1] {

    def @>^[P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om(RangeLibrary.Contains.column(n, Node(Library.Cast.column[B0](e.nodeDelegate))))
      }
    def @>[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.Contains.column(n, Node(e)))
      }
    def <@^:[P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om(RangeLibrary.ContainedBy.column(Node(Library.Cast.column[B0](e.nodeDelegate)), n))
      }
    def <@:[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.ContainedBy.column(Node(e), n))
      }
    def @&[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.Overlap.column(n, Node(e)))
      }
    def <<[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.StrictLeft.column(n, Node(e)))
      }
    def >>[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.StrictRight.column(n, Node(e)))
      }
    def &<[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.NotExtendRight.column(n, Node(e)))
      }
    def &>[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.NotExtendLeft.column(n, Node(e)))
      }
    def -|-[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.Adjacent.column(n, Node(e)))
      }

    def +[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Range[B0], R]) = {
        om(RangeLibrary.Union.column(n, Node(e)))
      }
    def *[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Range[B0], R]) = {
        om(RangeLibrary.Intersection.column(n, Node(e)))
      }
    def -[P2, R](e: Column[P2])(implicit om: o#arg[Range[B0], P2]#to[Range[B0], R]) = {
        om(RangeLibrary.Subtraction.column(n, Node(e)))
      }
  }

  ///////////////////////////////////////////////////////////////////////////////

  class RangeTypeMapper[T](rangeType: String, parseFn: (String => T))
              extends TypeMapperDelegate[Range[T]] with BaseTypeMapper[Range[T]] {

    def apply(v1: BasicProfile): TypeMapperDelegate[Range[T]] = this

    //-----------------------------------------------------------------
    def zero: Range[T] = null.asInstanceOf[Range[T]]

    def sqlType: Int = java.sql.Types.OTHER

    def sqlTypeName: String = rangeType

    def setValue(v: Range[T], p: PositionedParameters) = p.setObject(toPGObject(v), sqlType)

    def setOption(v: Option[Range[T]], p: PositionedParameters) = p.setObjectOption(v.map(toPGObject), sqlType)

    def nextValue(r: PositionedResult): Range[T] = r.nextStringOption().map(fromString).getOrElse(zero)

    def updateValue(v: Range[T], r: PositionedResult) = r.updateObject(toPGObject(v))

    override def valueToSQLLiteral(v: Range[T]) = v.toString

    ///
    private def toPGObject(v: Range[T]) = {
      val obj = new PGobject
      obj.setType(rangeType)
      obj.setValue(valueToSQLLiteral(v))
      obj
    }
    private def fromString(str: String): Range[T] = Range.fromString(parseFn)(str)
  }
}
