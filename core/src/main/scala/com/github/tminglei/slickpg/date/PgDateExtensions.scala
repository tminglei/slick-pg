package com.github.tminglei.slickpg
package date

import slick.ast.{Library, LiteralNode, TypedType}
import slick.ast.Library.{SqlFunction, SqlOperator}
import slick.lifted.ExtensionMethods
import slick.jdbc.{JdbcType, JdbcTypesComponent, PostgresProfile}

trait PgDateExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  import driver.api._

  object DateLibrary {
    val + = new SqlOperator("+")
    val - = new SqlOperator("-")
    val * = new SqlOperator("*")
    val / = new SqlOperator("/")

    val Age = new SqlFunction("age")
    val Part = new SqlFunction("date_part")
    val Trunc = new SqlFunction("date_trunc")
    val IsFinite = new SqlFunction("isfinite")

    val JustifyDays = new SqlFunction("justify_days")
    val JustifyHours = new SqlFunction("justify_hours")
    val JustifyInterval = new SqlFunction("justify_interval")

    val AtTimeZone = new SqlOperator("at time zone")
  }

  trait DateExtHelper[INTERVAL] {
    protected def toCastedIntervalNode[P](e: Rep[P])(implicit tm: JdbcType[INTERVAL]) = e.toNode match {
      case n: LiteralNode => Library.Cast.typed(tm, n, LiteralNode("interval"))
      case n              => n
    }
  }


  /// !!!NOTE: if `TIMESTAMP` is `timestamp with time zone`, `TIMESTAMP_TZ` should be `timestamp without time zone`
  class TimestampColumnExtensionMethods[DATE, TIME, TIMESTAMP, TIMESTAMP_TZ, INTERVAL, P1](val c: Rep[P1])(
            implicit tm: JdbcType[INTERVAL], tm1: JdbcType[DATE], tm2: JdbcType[TIME], tm3: JdbcType[TIMESTAMP], tm4: JdbcType[TIMESTAMP_TZ]
        ) extends ExtensionMethods[TIMESTAMP, P1] with DateExtHelper[INTERVAL] {

    protected implicit def b1Type: TypedType[TIMESTAMP] = implicitly[TypedType[TIMESTAMP]]

    def +++[P2, R](e: Rep[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.+, n, toCastedIntervalNode(e))
      }
    def - [P2, R](e: Rep[P2])(implicit om: o#to[INTERVAL, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def -- [P2, R](e: Rep[P2])(implicit om: o#arg[TIME, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def ---[P2, R](e: Rep[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.-, n, toCastedIntervalNode(e))
      }

    def age[R](implicit om: o#to[INTERVAL, R], tm: JdbcType[INTERVAL]) = om.column(DateLibrary.Age, n)
    def age[P2, R](e: Rep[P2])(implicit om: o#arg[TIMESTAMP, P2]#to[INTERVAL, R]) = {
        om.column(DateLibrary.Age, e.toNode, n)
      }
    def part[R](field: Rep[String])(implicit om: o#to[Double, R]) = {
        om.column(DateLibrary.Part, field.toNode, n)
      }
    def trunc[R](field: Rep[String])(implicit om: o#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.Trunc, field.toNode, n)
      }
    def isFinite[R](implicit om: o#to[Boolean, R]) = om.column(DateLibrary.IsFinite, n)

    def atTimeZone[R](tz: Rep[String])(implicit om: o#to[TIMESTAMP_TZ, R]) =
      om.column(DateLibrary.AtTimeZone, n, tz.toNode)
  }

  /// !!!NOTE: if `TIME` is `time with time zone`, `TIME_TZ` should be `time without time zone`
  class TimeColumnExtensionMethods[DATE, TIME, TIMESTAMP, TIME_TZ, INTERVAL, P1](val c: Rep[P1])(
            implicit tm: JdbcType[INTERVAL], tm1: JdbcType[DATE], tm2: JdbcType[TIME], tm3: JdbcType[TIMESTAMP], tm4: JdbcType[TIME_TZ]
        ) extends ExtensionMethods[TIME, P1] with DateExtHelper[INTERVAL] {

    protected implicit def b1Type: TypedType[TIME] = implicitly[TypedType[TIME]]

    def + [P2, R](e: Rep[P2])(implicit om: o#arg[DATE, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def +++[P2, R](e: Rep[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIME, R]) = {
        om.column(DateLibrary.+, n, toCastedIntervalNode(e))
      }
    def - [P2, R](e: Rep[P2])(implicit om: o#arg[TIME, P2]#to[INTERVAL, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def ---[P2, R](e: Rep[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIME, R]) = {
        om.column(DateLibrary.-, n, toCastedIntervalNode(e))
      }

    def atTimeZone[R](tz: Rep[String])(implicit om: o#to[TIME_TZ, R]) =
      om.column(DateLibrary.AtTimeZone, n, tz.toNode)
  }

  ///
  class DateColumnExtensionMethods[DATE, TIME, TIMESTAMP, INTERVAL, P1](val c: Rep[P1])(
              implicit tm: JdbcType[INTERVAL], tm1: JdbcType[DATE], tm2: JdbcType[TIME], tm3: JdbcType[TIMESTAMP]
        ) extends ExtensionMethods[DATE, P1] with DateExtHelper[INTERVAL] {

    protected implicit def b1Type: TypedType[DATE] = implicitly[TypedType[DATE]]

    def + [P2, R](e: Rep[P2])(implicit om: o#arg[TIME, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def ++ [P2, R](e: Rep[P2])(implicit om: o#arg[Int, P2]#to[DATE, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def +++[P2, R](e: Rep[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.+, n, toCastedIntervalNode(e))
      }
    def - [P2, R](e: Rep[P2])(implicit om: o#arg[DATE, P2]#to[Int, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def -- [P2, R](e: Rep[P2])(implicit om: o#arg[Int, P2]#to[DATE, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def ---[P2, R](e: Rep[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.-, n, toCastedIntervalNode(e))
      }
    def isFinite[R](implicit om: o#to[Boolean, R]) = om.column(DateLibrary.IsFinite, n)
  }

  ///
  class IntervalColumnExtensionMethods[DATE, TIME, TIMESTAMP, INTERVAL, P1](val c: Rep[P1])(
              implicit tm: JdbcType[INTERVAL], tm1: JdbcType[DATE], tm2: JdbcType[TIME], tm3: JdbcType[TIMESTAMP]
        ) extends ExtensionMethods[INTERVAL, P1] {

    protected implicit def b1Type: TypedType[INTERVAL] = implicitly[TypedType[INTERVAL]]

    def + [P2, R](e: Rep[P2])(implicit om: o#arg[INTERVAL, P2]#to[INTERVAL, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def unary_-[R](implicit om: o#to[INTERVAL, R]) = om.column(DateLibrary.-, n)
    def - [P2, R](e: Rep[P2])(implicit om: o#arg[INTERVAL, P2]#to[INTERVAL, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def * [R](factor: Rep[Double])(implicit om: o#to[INTERVAL, R]) = {
        om.column(DateLibrary.*, n, factor.toNode)
      }
    def / [R](factor: Rep[Double])(implicit om: o#to[INTERVAL, R]) = {
        om.column(DateLibrary./, n, factor.toNode)
      }

    def part[R](field: Rep[String])(implicit om: o#to[Double, R]) = {
        om.column(DateLibrary.Part, field.toNode, n)
      }
    def isFinite[R](implicit om: o#to[Boolean, R]) = om.column(DateLibrary.IsFinite, n)
    def justifyDays[R](implicit om: o#to[INTERVAL, R]) = om.column(DateLibrary.JustifyDays, n)
    def justifyHours[R](implicit om: o#to[INTERVAL, R]) = om.column(DateLibrary.JustifyHours, n)
    def justifyInterval[R](implicit om: o#to[INTERVAL, R]) = om.column(DateLibrary.JustifyInterval, n)
  }
}
