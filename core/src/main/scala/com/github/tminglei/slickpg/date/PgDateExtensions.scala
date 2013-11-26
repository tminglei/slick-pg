package com.github.tminglei.slickpg
package date

import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.lifted.{ExtensionMethods, Column}
import scala.slick.jdbc.JdbcType

trait PgDateExtensions extends utils.ImplicitJdbcTypes {

  type DATE
  type TIME
  type TIMESTAMP
  type INTERVAL

  object DateLibrary {
    val + = new SqlOperator("+")
    val - = new SqlOperator("-")
    val * = new SqlOperator("*")
    val / = new SqlOperator("/")

    val Age = new SqlFunction("age")
    val Part = new SqlFunction("date_part")
    val Trunc = new SqlFunction("date_trunc")

    val Negative = new SqlOperator("-")
    val JustifyDays = new SqlFunction("justify_days")
    val JustifyHours = new SqlFunction("justify_hours")
    val JustifyInterval = new SqlFunction("justify_interval")
  }

  class TimestampColumnExtensionMethods[P1](val c: Column[P1])(
            implicit tm: JdbcType[INTERVAL], tm1: JdbcType[DATE], tm2: JdbcType[TIME], tm3: JdbcType[TIMESTAMP])
                  extends ExtensionMethods[TIMESTAMP, P1] {

    def +++[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def - [P2, R](e: Column[P2])(implicit om: o#to[INTERVAL, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def -- [P2, R](e: Column[P2])(implicit om: o#arg[TIME, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def ---[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }

    def age[R](implicit om: o#to[INTERVAL, R]) = om.column(DateLibrary.Age, n)
    def age[P2, R](e: Column[P2])(implicit om: o#arg[TIMESTAMP, P2]#to[INTERVAL, R]) = {
        om.column(DateLibrary.Age, e.toNode, n)
      }
    def part[R](field: Column[String])(implicit om: o#to[Double, R]) = {
        om.column(DateLibrary.Part, field.toNode, n)
      }
    def trunc[R](field: Column[String])(implicit om: o#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.Trunc, field.toNode, n)
      }
  }

  class TimeColumnExtensionMethods[P1](val c: Column[P1])(
            implicit tm: JdbcType[INTERVAL], tm1: JdbcType[DATE], tm2: JdbcType[TIME], tm3: JdbcType[TIMESTAMP])
                  extends ExtensionMethods[TIME, P1] {

    def + [P2, R](e: Column[P2])(implicit om: o#arg[DATE, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def +++[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIME, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def - [P2, R](e: Column[P2])(implicit om: o#arg[TIME, P2]#to[INTERVAL, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def ---[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIME, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
  }

  class DateColumnExtensionMethods[P1](val c: Column[P1])(
              implicit tm: JdbcType[INTERVAL], tm1: JdbcType[DATE], tm2: JdbcType[TIME], tm3: JdbcType[TIMESTAMP])
                    extends ExtensionMethods[DATE, P1] {

    def + [P2, R](e: Column[P2])(implicit om: o#arg[TIME, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def ++ [P2, R](e: Column[P2])(implicit om: o#arg[Int, P2]#to[DATE, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def +++[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def - [P2, R](e: Column[P2])(implicit om: o#arg[DATE, P2]#to[Int, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def -- [P2, R](e: Column[P2])(implicit om: o#arg[Int, P2]#to[DATE, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def ---[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIMESTAMP, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
  }

  class IntervalColumnExtensionMethods[P1](val c: Column[P1])(
              implicit tm: JdbcType[INTERVAL], tm1: JdbcType[DATE], tm2: JdbcType[TIME], tm3: JdbcType[TIMESTAMP])
                    extends ExtensionMethods[INTERVAL, P1] {

    def + [P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[INTERVAL, R]) = {
        om.column(DateLibrary.+, n, e.toNode)
      }
    def unary_-[R](implicit om: o#to[INTERVAL, R]) = om.column(DateLibrary.Negative, n)
    def - [P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[INTERVAL, R]) = {
        om.column(DateLibrary.-, n, e.toNode)
      }
    def * [R](factor: Column[Double])(implicit om: o#to[INTERVAL, R]) = {
        om.column(DateLibrary.*, n, factor.toNode)
      }
    def / [R](factor: Column[Double])(implicit om: o#to[INTERVAL, R]) = {
        om.column(DateLibrary./, n, factor.toNode)
      }

    def part[R](field: Column[String])(implicit om: o#to[Double, R]) = {
        om.column(DateLibrary.Part, field.toNode, n)
      }
    def justifyDays[R](implicit om: o#to[INTERVAL, R]) = om.column(DateLibrary.JustifyDays, n)
    def justifyHours[R](implicit om: o#to[INTERVAL, R]) = om.column(DateLibrary.JustifyHours, n)
    def justifyInterval[R](implicit om: o#to[INTERVAL, R]) = om.column(DateLibrary.JustifyInterval, n)
  }
}
