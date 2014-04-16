package com.github.tminglei.slickpg
package hstore

import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.lifted.{FunctionSymbolExtensionMethods, ExtensionMethods, Column}
import scala.slick.driver.{JdbcTypesComponent, PostgresDriver}
import scala.slick.ast.Library
import scala.slick.jdbc.JdbcType

trait PgHStoreExtensions extends JdbcTypesComponent { driver: PostgresDriver =>
  import driver.Implicit._
  import FunctionSymbolExtensionMethods._

  object HStoreLibrary {
    val On = new SqlOperator("->")
    val Exist   = new SqlFunction("exist")
//    val ExistAll = new SqlOperator("?&")  //can't support, '?' conflict with jdbc '?'
//    val ExistAny = new SqlOperator("?|")  //can't support, '?' conflict with jdbc '?'
    val Defined = new SqlFunction("defined")
    val Contains = new SqlOperator("@>")
    val ContainedBy = new SqlOperator("<@")

    val Concatenate = new SqlOperator("||")
    val Delete = new SqlOperator("-")
  }

  /** Extension methods for hstore Columns */
  class HStoreColumnExtensionMethods[P1](val c: Column[P1])(
              implicit tm: JdbcType[Map[String, String]], tm1: JdbcType[List[String]])
                    extends ExtensionMethods[Map[String, String], P1] {

    def +>[P2, R](k: Column[P2])(implicit om: o#arg[String, P2]#to[String, R]) = {
        om.column(HStoreLibrary.On, n, k.toNode)
      }
    def >>[T: JdbcType](k: Column[String]) = {
        Library.Cast.column[T](HStoreLibrary.On.column[String](n, k.toNode).toNode)
      }
    def ??[P2, R](k: Column[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(HStoreLibrary.Exist, n, k.toNode)
      }
    def ?&[P2, R](k: Column[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(HStoreLibrary.Defined, n, k.toNode)
      }
    def @>[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Boolean, R]) = {
        om.column(HStoreLibrary.Contains, n, c2.toNode)
      }
    def <@:[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Boolean, R]) = {
        om.column(HStoreLibrary.ContainedBy, c2.toNode, n)
      }

    def @+[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Map[String, String], R]) = {
        om.column(HStoreLibrary.Concatenate, n, c2.toNode)
      }
    def @-[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Map[String, String], R]) = {
        om.column(HStoreLibrary.Delete, n, c2.toNode)
      }
    def --[P2, R](c2: Column[P2])(implicit om: o#arg[List[String], P2]#to[Map[String, String], R]) = {
        om.column(HStoreLibrary.Delete, n, c2.toNode)
      }
    def -/[P2, R](c2: Column[P2])(implicit om: o#arg[String, P2]#to[Map[String, String], R]) = {
        om.column(HStoreLibrary.Delete, n, c2.toNode)
      }
  }
}
