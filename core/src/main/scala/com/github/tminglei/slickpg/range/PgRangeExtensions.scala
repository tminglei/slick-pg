package com.github.tminglei.slickpg
package range

import scala.slick.ast.Library.SqlOperator
import scala.slick.lifted.{FunctionSymbolExtensionMethods, ExtensionMethods, Column}
import scala.slick.driver.{JdbcTypesComponent, PostgresDriver}
import scala.slick.ast.Library
import scala.slick.jdbc.JdbcType

trait PgRangeExtensions extends JdbcTypesComponent { driver: PostgresDriver =>
  import driver.Implicit._
  import FunctionSymbolExtensionMethods._

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

  class RangeColumnExtensionMethods[RANGEType[_], B0, P1](val c: Column[P1])(
              implicit tm: JdbcType[B0], tm1: JdbcType[RANGEType[B0]]) extends ExtensionMethods[RANGEType[B0], P1] {

    def @>^[P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.Contains, n, Library.Cast.column[B0](e.toNode).toNode)
      }
    def @>[P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.Contains, n, e.toNode)
      }
    def <@^:[P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.ContainedBy, Library.Cast.column[B0](e.toNode).toNode, n)
      }
    def <@:[P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.ContainedBy, e.toNode, n)
      }
    def @&[P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.Overlap, n, e.toNode)
      }
    def <<[P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.StrictLeft, n, e.toNode)
      }
    def >>[P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.StrictRight, n, e.toNode)
      }
    def &<[P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.NotExtendRight, n, e.toNode)
      }
    def &>[P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.NotExtendLeft, n, e.toNode)
      }
    def -|-[P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.Adjacent, n, e.toNode)
      }

    def + [P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[RANGEType[B0], R]) = {
        om.column(RangeLibrary.Union, n, e.toNode)
      }
    def * [P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[RANGEType[B0], R]) = {
        om.column(RangeLibrary.Intersection, n, e.toNode)
      }
    def - [P2, R](e: Column[P2])(implicit om: o#arg[RANGEType[B0], P2]#to[RANGEType[B0], R]) = {
        om.column(RangeLibrary.Subtraction, n, e.toNode)
      }
  }
}
