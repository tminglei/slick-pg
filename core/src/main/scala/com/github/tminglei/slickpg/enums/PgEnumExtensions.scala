package com.github.tminglei.slickpg
package enums

import slick.ast.TypedType
import slick.ast.Library.SqlFunction
import slick.lifted.ExtensionMethods
import slick.jdbc.{JdbcType, JdbcTypesComponent, PostgresProfile}

trait PgEnumExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  import driver.api._

  object EnumLibrary {
    val first = new SqlFunction("enum_first")
    val last  = new SqlFunction("enum_last")
    val range = new SqlFunction("enum_range")
  }

  class EnumColumnExtensionMethods[B0, P1](val c: Rep[P1])(
            implicit tm: JdbcType[B0], tm1: JdbcType[List[B0]]) extends ExtensionMethods[B0, P1] {

    protected implicit def b1Type: TypedType[B0] = implicitly[TypedType[B0]]

    def first[R](implicit om: o#to[B0, R]) = om.column(EnumLibrary.first, n)
    def last[R](implicit om: o#to[B0, R]) = om.column(EnumLibrary.last, n)
    def all[R](implicit om: o#to[List[B0], R]) = om.column(EnumLibrary.range, n)
    def range[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[List[B0], R]) = {
        om.column(EnumLibrary.range, n, e.toNode)
      }
  }
}
