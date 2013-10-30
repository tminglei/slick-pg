package com.github.tminglei.slickpg.range

import scala.slick.ast.Library.SqlOperator
import scala.slick.lifted.{ExtensionMethods, TypeMapper, Column}
import scala.slick.ast.{Library, Node}

trait PgRangeExtensions {

  type RangeType[T]

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
}
