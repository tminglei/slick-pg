package com.github.tminglei.slickpg
package array

import scala.slick.ast.Library.{SqlOperator, SqlFunction}
import scala.slick.lifted.{ConstColumn, ExtensionMethods, TypeMapper, Column}
import scala.slick.ast.Node

trait PgArrayExtensions {
  
  object ArrayLibrary {
    val Any = new SqlFunction("any")
    val All = new SqlFunction("all")
    val Concatenate = new SqlOperator("||")
    val Contains  = new SqlOperator("@>")
    val ContainedBy = new SqlOperator("<@")
    val Overlap = new SqlOperator("&&")

    val Length = new SqlFunction("array_length")
    val Unnest = new SqlFunction("unnest")
  }

  /** Extension methods for array Columns */
  class ArrayColumnExtensionMethods[B0, P1](val c: Column[P1])(
            implicit tm0: TypeMapper[B0], tm: TypeMapper[List[B0]]) extends ExtensionMethods[List[B0], P1] {
    /** required syntax: expression operator ANY (array expression) */
    def any[R](implicit om: o#to[B0, R]) = om(ArrayLibrary.Any.column[B0](n))
    /** required syntax: expression operator ALL (array expression) */
    def all[R](implicit om: o#to[B0, R]) = om(ArrayLibrary.All.column[B0](n))

    def @>[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[Boolean, R]) = {
        om(ArrayLibrary.Contains.column(n, Node(e)))
      }
    def <@:[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[Boolean, R]) = {
        om(ArrayLibrary.ContainedBy.column(Node(e), n))
      }
    def @&[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[Boolean, R]) = {
        om(ArrayLibrary.Overlap.column(n, Node(e)))
      }

    def ++[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[List[B0], R]) = {
        om(ArrayLibrary.Concatenate.column(n, Node(e)))
      }
    def + [P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[List[B0], R]) = {
        om(ArrayLibrary.Concatenate.column(n, Node(e)))
      }
    def +:[P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[List[B0], R]) = {
        om(ArrayLibrary.Concatenate.column(Node(e), n))
      }
    def length(dim: Column[Int] = ConstColumn(1)) = ArrayLibrary.Length.column[Int](n, Node(dim))
    def unnest[R](implicit om: o#to[B0, R]) = om(ArrayLibrary.Unnest.column(n))
  }  
}
