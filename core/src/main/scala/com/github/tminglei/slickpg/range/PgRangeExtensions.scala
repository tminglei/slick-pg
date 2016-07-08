package com.github.tminglei.slickpg
package range

import slick.ast.TypedType
import slick.ast.Library.{SqlFunction, SqlOperator}
import slick.lifted.{ExtensionMethods, FunctionSymbolExtensionMethods}
import slick.ast.Library
import slick.jdbc.{JdbcType, JdbcTypesComponent, PostgresProfile}

trait PgRangeExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  import driver.api._
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

    val Lower = new SqlFunction("lower")
    val Upper = new SqlFunction("upper")
  }

  class RangeColumnExtensionMethods[RANGEType, B0, P1](val c: Rep[P1])(
              implicit tm: JdbcType[B0], tm1: JdbcType[RANGEType]) extends ExtensionMethods[RANGEType, P1] {

    protected implicit def b1Type: TypedType[RANGEType] = implicitly[TypedType[RANGEType]]

    def @>^[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.Contains, n, Library.Cast.column[B0](e.toNode).toNode)
      }
    def @>[P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.Contains, n, e.toNode)
      }
    def <@^:[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.ContainedBy, Library.Cast.column[B0](e.toNode).toNode, n)
      }
    def <@:[P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.ContainedBy, e.toNode, n)
      }
    def @&[P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.Overlap, n, e.toNode)
      }
    def <<[P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.StrictLeft, n, e.toNode)
      }
    def >>[P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.StrictRight, n, e.toNode)
      }
    def &<[P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.NotExtendRight, n, e.toNode)
      }
    def &>[P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.NotExtendLeft, n, e.toNode)
      }
    def -|-[P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[Boolean, R]) = {
        om.column(RangeLibrary.Adjacent, n, e.toNode)
      }

    def + [P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[RANGEType, R]) = {
        om.column(RangeLibrary.Union, n, e.toNode)
      }
    def * [P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[RANGEType, R]) = {
        om.column(RangeLibrary.Intersection, n, e.toNode)
      }
    def - [P2, R](e: Rep[P2])(implicit om: o#arg[RANGEType, P2]#to[RANGEType, R]) = {
        om.column(RangeLibrary.Subtraction, n, e.toNode)
      }

    def lower[R](implicit om: o#to[B0, R]) = om.column(RangeLibrary.Lower, n)
    def upper[R](implicit om: o#to[B0, R]) = om.column(RangeLibrary.Upper, n)
  }
}
