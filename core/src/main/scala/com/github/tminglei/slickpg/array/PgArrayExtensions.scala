package com.github.tminglei.slickpg
package array

import scala.slick.ast.Library.{SqlOperator, SqlFunction}
import scala.slick.lifted.{LiteralColumn, ExtensionMethods, Column}
import scala.slick.driver.{JdbcTypesComponent, PostgresDriver}
import scala.slick.jdbc.JdbcType

trait PgArrayExtensions extends JdbcTypesComponent { driver: PostgresDriver =>
  import driver.Implicit._

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
            implicit tm0: JdbcType[B0], tm: JdbcType[List[B0]]) extends ExtensionMethods[List[B0], P1] {
    /** required syntax: expression operator ANY (array expression) */
    def any[R](implicit om: o#to[B0, R]) = om.column(ArrayLibrary.Any, n)
    /** required syntax: expression operator ALL (array expression) */
    def all[R](implicit om: o#to[B0, R]) = om.column(ArrayLibrary.All, n)

    def @>[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[Boolean, R]) = {
        om.column(ArrayLibrary.Contains, n, e.toNode)
      }
    def <@:[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[Boolean, R]) = {
        om.column(ArrayLibrary.ContainedBy, e.toNode, n)
      }
    def @&[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[Boolean, R]) = {
        om.column(ArrayLibrary.Overlap, n, e.toNode)
      }

    def ++[P2, R](e: Column[P2])(implicit om: o#arg[List[B0], P2]#to[List[B0], R]) = {
        om.column(ArrayLibrary.Concatenate, n, e.toNode)
      }
    def + [P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[List[B0], R]) = {
        om.column(ArrayLibrary.Concatenate, n, e.toNode)
      }
    def +:[P2, R](e: Column[P2])(implicit om: o#arg[B0, P2]#to[List[B0], R]) = {
        om.column(ArrayLibrary.Concatenate, e.toNode, n)
      }
    def length[R](dim: Column[Int] = LiteralColumn(1))(implicit om: o#to[Int, R]) = {
        om.column(ArrayLibrary.Length, n, dim.toNode)
      }
    def unnest[R](implicit om: o#to[B0, R]) = om.column(ArrayLibrary.Unnest, n)
  }  
}
