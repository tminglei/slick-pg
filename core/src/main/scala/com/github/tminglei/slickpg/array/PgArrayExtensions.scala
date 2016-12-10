package com.github.tminglei.slickpg
package array

import slick.ast.TypedType
import slick.ast.Library.{SqlFunction, SqlOperator}
import slick.lifted.ExtensionMethods
import slick.jdbc.{JdbcType, JdbcTypesComponent, PostgresProfile}

trait PgArrayExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  import driver.api._

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
  class ArrayColumnExtensionMethods[B0, SEQ[B0], P1](val c: Rep[P1])(
            implicit tm0: JdbcType[B0], tm: JdbcType[SEQ[B0]]) extends ExtensionMethods[SEQ[B0], P1] {

    protected implicit def b1Type: TypedType[SEQ[B0]] = implicitly[TypedType[SEQ[B0]]]

    /** required syntax: expression operator ANY (array expression) */
    def any[R](implicit om: o#to[B0, R]) = om.column(ArrayLibrary.Any, n)
    /** required syntax: expression operator ALL (array expression) */
    def all[R](implicit om: o#to[B0, R]) = om.column(ArrayLibrary.All, n)

    def @>[P2, R](e: Rep[P2])(implicit om: o#arg[SEQ[B0], P2]#to[Boolean, R]) = {
        om.column(ArrayLibrary.Contains, n, e.toNode)
      }
    def <@:[P2, R](e: Rep[P2])(implicit om: o#arg[SEQ[B0], P2]#to[Boolean, R]) = {
        om.column(ArrayLibrary.ContainedBy, e.toNode, n)
      }
    def @&[P2, R](e: Rep[P2])(implicit om: o#arg[SEQ[B0], P2]#to[Boolean, R]) = {
        om.column(ArrayLibrary.Overlap, n, e.toNode)
      }

    def ++[P2, R](e: Rep[P2])(implicit om: o#arg[SEQ[B0], P2]#to[SEQ[B0], R]) = {
        om.column(ArrayLibrary.Concatenate, n, e.toNode)
      }
    def + [P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[SEQ[B0], R]) = {
        om.column(ArrayLibrary.Concatenate, n, e.toNode)
      }
    def +:[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[SEQ[B0], R]) = {
        om.column(ArrayLibrary.Concatenate, e.toNode, n)
      }
    def length[R](dim: Rep[Int] = LiteralColumn(1))(implicit om: o#to[Int, R]) = {
        om.column(ArrayLibrary.Length, n, dim.toNode)
      }
    def unnest[R](implicit om: o#to[B0, R]) = om.column(ArrayLibrary.Unnest, n)
  }
}
