package com.github.tminglei.slickpg

import scala.slick.driver.{BasicProfile, PostgresDriver}
import scala.slick.lifted._
import scala.slick.session.{PositionedResult, PositionedParameters}
import org.postgresql.util.PGobject
import java.sql.{Date, Timestamp}
import scala.slick.ast.Library.SqlOperator
import scala.slick.ast.{Library, Node}

trait PgRangeSupport { driver: PostgresDriver =>

  type RangeType[T]

  trait RangeImplicits {
    // !!!NOTE: supplement RangeTypeMapper declarations when your using, like this
//    implicit val intRangeTypeMapper = new RangeTypeMapper(classOf[Int], Range.mkParser(_.toInt))
//    implicit val longRangeTypeMapper = new RangeTypeMapper(classOf[Long], Range.mkParser(_.toLong))

    implicit def rangeColumnExtensionMethods[B0, RangeType[B0]](c: Column[RangeType[B0]])(
      implicit tm: TypeMapper[B0], tm1: RangeTypeMapper[B0]) = {
        new RangeColumnExtensionMethods[B0, RangeType[B0]](c)
      }
    implicit def rangeOptionColumnExtensionMethods[B0, RangeType[B0]](c: Column[Option[RangeType[B0]]])(
      implicit tm: TypeMapper[B0], tm1: RangeTypeMapper[B0]) = {
        new RangeColumnExtensionMethods[B0, Option[RangeType[B0]]](c)
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
              implicit tm: TypeMapper[B0], tm1: TypeMapper[RangeType[B0]]) extends ExtensionMethods[RangeType[B0], P1] {

    def @>^[P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om(RangeLibrary.Contains.column(n, Node(Library.Cast.column[B0](e.nodeDelegate))))
      }
    def @>[P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.Contains.column(n, Node(e)))
      }
    def <@^:[P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om(RangeLibrary.ContainedBy.column(Node(Library.Cast.column[B0](e.nodeDelegate)), n))
      }
    def <@:[P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.ContainedBy.column(Node(e), n))
      }
    def @&[P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.Overlap.column(n, Node(e)))
      }
    def <<[P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.StrictLeft.column(n, Node(e)))
      }
    def >>[P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.StrictRight.column(n, Node(e)))
      }
    def &<[P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.NotExtendRight.column(n, Node(e)))
      }
    def &>[P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.NotExtendLeft.column(n, Node(e)))
      }
    def -|-[P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[Boolean, R]) = {
        om(RangeLibrary.Adjacent.column(n, Node(e)))
      }

    def + [P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[RangeType[B0], R]) = {
        om(RangeLibrary.Union.column(n, Node(e)))
      }
    def * [P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[RangeType[B0], R]) = {
        om(RangeLibrary.Intersection.column(n, Node(e)))
      }
    def - [P2, R](e: Column[P2])(implicit om: o#arg[RangeType[B0], P2]#to[RangeType[B0], R]) = {
        om(RangeLibrary.Subtraction.column(n, Node(e)))
      }
  }

  ///////////////////////////////////////////////////////////////////////////////

  val pgRangeTypes: Map[Class[_], String] = Map(
                          classOf[Int] -> "int4range",
                          classOf[Long] -> "int8range",
                          classOf[Float] -> "numrange",
                          classOf[Timestamp] -> "tsrange",
                          classOf[Date] -> "daterange")

  ///
  class RangeTypeMapper[T](baseType: Class[T], fnFromString: (String => RangeType[T]),
                           fnToString: (RangeType[T] => String) = ((r: RangeType[T]) => r.toString))
                                extends TypeMapperDelegate[RangeType[T]] with BaseTypeMapper[RangeType[T]] {

    def apply(v1: BasicProfile): TypeMapperDelegate[RangeType[T]] = this

    //-----------------------------------------------------------------
    def zero: RangeType[T] = null.asInstanceOf[RangeType[T]]

    def sqlType: Int = java.sql.Types.OTHER

    def sqlTypeName: String = pgRangeTypes.get(baseType)
      .getOrElse(throw new NotImplementedError(s"Unsupported base type: ${baseType.getName}, pls check/override PgRangeSupport.pgRangeTypes."))

    def setValue(v: RangeType[T], p: PositionedParameters) = p.setObject(mkPgObject(v), sqlType)

    def setOption(v: Option[RangeType[T]], p: PositionedParameters) = p.setObjectOption(v.map(mkPgObject), sqlType)

    def nextValue(r: PositionedResult): RangeType[T] = r.nextStringOption().map(fnFromString).getOrElse(zero)

    def updateValue(v: RangeType[T], r: PositionedResult) = r.updateObject(mkPgObject(v))

    override def valueToSQLLiteral(v: RangeType[T]) = fnToString(v)

    ///
    private def mkPgObject(v: RangeType[T]) = {
      val obj = new PGobject
      obj.setType(sqlTypeName)
      obj.setValue(valueToSQLLiteral(v))
      obj
    }
  }
}
