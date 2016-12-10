package com.github.tminglei.slickpg
package hstore

import slick.ast.TypedType
import slick.ast.Library.{SqlFunction, SqlOperator}
import slick.lifted.{ExtensionMethods, FunctionSymbolExtensionMethods}
import slick.ast.Library
import slick.jdbc.{JdbcType, JdbcTypesComponent, PostgresProfile}

trait PgHStoreExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  import driver.api._
  import FunctionSymbolExtensionMethods._

  object HStoreLibrary {
    val On = new SqlOperator("->")
    val Exist   = new SqlOperator("??")
    val ExistAll = new SqlOperator("??&")
    val ExistAny = new SqlOperator("??|")
    val Defined = new SqlFunction("defined")
    val Contains = new SqlOperator("@>")
    val ContainedBy = new SqlOperator("<@")

    val Concatenate = new SqlOperator("||")
    val Delete = new SqlOperator("-")
    val Slice = new SqlFunction("slice")
  }

  /** Extension methods for hstore Columns */
  class HStoreColumnExtensionMethods[P1](val c: Rep[P1])(
              implicit tm: JdbcType[Map[String, String]], tm1: JdbcType[List[String]])
                    extends ExtensionMethods[Map[String, String], P1] {

    protected implicit def b1Type: TypedType[Map[String, String]] = implicitly[TypedType[Map[String, String]]]

    def +>[P2, R](k: Rep[P2])(implicit om: o#arg[String, P2]#to[String, R]) = {
        om.column(HStoreLibrary.On, n, k.toNode)
      }
    def >>[T: JdbcType](k: Rep[String]) = {
        Library.Cast.column[T](HStoreLibrary.On.column[String](n, k.toNode).toNode)
      }
    def ??[P2, R](k: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(HStoreLibrary.Exist, n, k.toNode)
      }
    def ?*[P2, R](k: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(HStoreLibrary.Defined, n, k.toNode)
      }
    def ?|[P2, R](k: Rep[P2])(implicit om: o#arg[List[String], P2]#to[Boolean, R]) = {
        om.column(HStoreLibrary.ExistAny, n, k.toNode)
      }
    def ?&[P2, R](k: Rep[P2])(implicit om: o#arg[List[String], P2]#to[Boolean, R]) = {
        om.column(HStoreLibrary.ExistAll, n, k.toNode)
      }
    def @>[P2, R](c2: Rep[P2])(implicit om: o#arg[Map[String, String], P2]#to[Boolean, R]) = {
        om.column(HStoreLibrary.Contains, n, c2.toNode)
      }
    def <@:[P2, R](c2: Rep[P2])(implicit om: o#arg[Map[String, String], P2]#to[Boolean, R]) = {
        om.column(HStoreLibrary.ContainedBy, c2.toNode, n)
      }

    def @+[P2, R](c2: Rep[P2])(implicit om: o#arg[Map[String, String], P2]#to[Map[String, String], R]) = {
        om.column(HStoreLibrary.Concatenate, n, c2.toNode)
      }
    def @-[P2, R](c2: Rep[P2])(implicit om: o#arg[Map[String, String], P2]#to[Map[String, String], R]) = {
        om.column(HStoreLibrary.Delete, n, c2.toNode)
      }
    def --[P2, R](c2: Rep[P2])(implicit om: o#arg[List[String], P2]#to[Map[String, String], R]) = {
        om.column(HStoreLibrary.Delete, n, c2.toNode)
      }
    def -/[P2, R](c2: Rep[P2])(implicit om: o#arg[String, P2]#to[Map[String, String], R]) = {
        om.column(HStoreLibrary.Delete, n, c2.toNode)
      }
    def slice[P2, R](c2: Rep[P2])(implicit om: o#arg[List[String], P2]#to[Map[String, String], R]) = {
        om.column(HStoreLibrary.Slice, n, c2.toNode)
      }
  }
}
