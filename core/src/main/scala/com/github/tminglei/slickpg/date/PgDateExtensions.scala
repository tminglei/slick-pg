package com.github.tminglei.slickpg
package date

import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.lifted.{ExtensionMethods, TypeMapper, Column}
import scala.slick.ast.Node

trait PgDateExtensions {

  type DATE
  type TIME
  type TIMESTAMP
  type INTERVAL
  // timestamp with time zone
  type TIMESTAMP_TZ

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

  ///
  trait TimestampExtensionMethods[TS, P1] extends ExtensionMethods[TS, P1] {

    def +++[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TS, R]) = {
        om(DateLibrary.+.column(n, Node(e)))
      }
    def - [P2, R](e: Column[P2])(implicit om: o#to[INTERVAL, R], tm: TypeMapper[INTERVAL]) = {
        om(DateLibrary.-.column(n, Node(e)))
      }
    def -- [P2, R](e: Column[P2])(implicit om: o#arg[TIME, P2]#to[TS, R]) = {
        om(DateLibrary.-.column(n, Node(e)))
      }
    def ---[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TS, R]) = {
        om(DateLibrary.-.column(n, Node(e)))
      }

    def age[R](implicit om: o#to[INTERVAL, R], tm: TypeMapper[INTERVAL]) = om(DateLibrary.Age.column(n))
    def age[P2, R](e: Column[P2])(implicit om: o#arg[TS, P2]#to[INTERVAL, R], tm: TypeMapper[INTERVAL]) = {
        om(DateLibrary.Age.column(Node(e), n))
      }
    def part[R](field: Column[String])(implicit om: o#to[Double, R]) = {
        om(DateLibrary.Part.column(Node(field), n))
      }
    def trunc[R](field: Column[String])(implicit om: o#to[TS, R]) = {
        om(DateLibrary.Trunc.column(Node(field), n))
      }
  }

  class TimestampColumnExtensionMethods[P1](val c: Column[P1])(
          implicit tm: TypeMapper[INTERVAL], tm1: TypeMapper[DATE], tm2: TypeMapper[TIME], tm3: TypeMapper[TIMESTAMP])
                extends TimestampExtensionMethods[TIMESTAMP, P1]

  class TimestampTZColumnExtensionMethods[P1](val c: Column[P1])(
          implicit tm: TypeMapper[INTERVAL], tm1: TypeMapper[DATE], tm2: TypeMapper[TIME], tm3: TypeMapper[TIMESTAMP_TZ])
                extends TimestampExtensionMethods[TIMESTAMP_TZ, P1]

  ///
  class TimeColumnExtensionMethods[P1](val c: Column[P1])(
            implicit tm: TypeMapper[INTERVAL], tm1: TypeMapper[DATE], tm2: TypeMapper[TIME], tm3: TypeMapper[TIMESTAMP])
                  extends ExtensionMethods[TIME, P1] {

    def + [P2, R](e: Column[P2])(implicit om: o#arg[DATE, P2]#to[TIMESTAMP, R]) = {
        om(DateLibrary.+.column[TIMESTAMP](n, Node(e)))
      }
    def +++[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIME, R]) = {
        om(DateLibrary.+.column(n, Node(e)))
      }
    def - [P2, R](e: Column[P2])(implicit om: o#arg[TIME, P2]#to[INTERVAL, R]) = {
        om(DateLibrary.-.column(n, Node(e)))
      }
    def ---[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIME, R]) = {
        om(DateLibrary.-.column(n, Node(e)))
      }
  }

  ///
  class DateColumnExtensionMethods[P1](val c: Column[P1])(
              implicit tm: TypeMapper[INTERVAL], tm1: TypeMapper[DATE], tm2: TypeMapper[TIME], tm3: TypeMapper[TIMESTAMP])
                    extends ExtensionMethods[DATE, P1] {

    def + [P2, R](e: Column[P2])(implicit om: o#arg[TIME, P2]#to[TIMESTAMP, R]) = {
        om(DateLibrary.+.column[TIMESTAMP](n, Node(e)))
      }
    def ++ [P2, R](e: Column[P2])(implicit om: o#arg[Int, P2]#to[DATE, R]) = {
        om(DateLibrary.+.column(n, Node(e)))
      }
    def +++[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIMESTAMP, R]) = {
        om(DateLibrary.+.column[TIMESTAMP](n, Node(e)))
      }
    def - [P2, R](e: Column[P2])(implicit om: o#arg[DATE, P2]#to[Int, R]) = {
        om(DateLibrary.-.column(n, Node(e)))
      }
    def -- [P2, R](e: Column[P2])(implicit om: o#arg[Int, P2]#to[DATE, R]) = {
        om(DateLibrary.-.column(n, Node(e)))
      }
    def ---[P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[TIMESTAMP, R]) = {
        om(DateLibrary.-.column[TIMESTAMP](n, Node(e)))
      }
  }

  ///
  class IntervalColumnExtensionMethods[P1](val c: Column[P1])(
              implicit tm: TypeMapper[INTERVAL], tm1: TypeMapper[DATE], tm2: TypeMapper[TIME], tm3: TypeMapper[TIMESTAMP])
                    extends ExtensionMethods[INTERVAL, P1] {

    def + [P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[INTERVAL, R]) = {
        om(DateLibrary.+.column(n, Node(e)))
      }
    def unary_-[R](implicit om: o#to[INTERVAL, R]) = om(DateLibrary.Negative.column(n))
    def - [P2, R](e: Column[P2])(implicit om: o#arg[INTERVAL, P2]#to[INTERVAL, R]) = {
        om(DateLibrary.-.column(n, Node(e)))
      }
    def * [R](factor: Column[Double])(implicit om: o#to[INTERVAL, R]) = {
        om(DateLibrary.*.column(n, Node(factor)))
      }
    def / [R](factor: Column[Double])(implicit om: o#to[INTERVAL, R]) = {
        om(DateLibrary./.column(n, Node(factor)))
      }

    def part[R](field: Column[String])(implicit om: o#to[Double, R]) = {
        om(DateLibrary.Part.column(Node(field), n))
      }
    def justifyDays[R](implicit om: o#to[INTERVAL, R]) = om(DateLibrary.JustifyDays.column(n))
    def justifyHours[R](implicit om: o#to[INTERVAL, R]) = om(DateLibrary.JustifyHours.column(n))
    def justifyInterval[R](implicit om: o#to[INTERVAL, R]) = om(DateLibrary.JustifyInterval.column(n))
  }
}
