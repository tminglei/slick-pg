package com.github.tminglei.slickpg
package hstore

import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.lifted.{ExtensionMethods, TypeMapper, Column}
import scala.slick.ast.{Library, Node}

trait PgHStoreExtensions {

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
              implicit tm: TypeMapper[Map[String, String]], tm1: TypeMapper[List[String]]) extends ExtensionMethods[Map[String, String], P1] {

    def +>[P2, R](k: Column[P2])(implicit om: o#arg[String, P2]#to[String, R]) = {
        om(HStoreLibrary.On.column[String](n, Node(k)))
      }
    def >>[T: TypeMapper](k: Column[String]) = {
        Library.Cast.column[T](HStoreLibrary.On(n, Node(k)))
      }
    def ??[P2, R](k: Column[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om(HStoreLibrary.Exist.column(n, Node(k)))
      }
    def ?&[P2, R](k: Column[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om(HStoreLibrary.Defined.column(n, Node(k)))
      }
    def @>[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Boolean, R]) = {
        om(HStoreLibrary.Contains.column(n, Node(c2)))
      }
    def <@:[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Boolean, R]) = {
        om(HStoreLibrary.ContainedBy.column(Node(c2), n))
      }

    def @+[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Map[String, String], R]) = {
        om(HStoreLibrary.Concatenate.column[Map[String, String]](n, Node(c2)))
      }
    def @-[P2, R](c2: Column[P2])(implicit om: o#arg[Map[String, String], P2]#to[Map[String, String], R]) = {
        om(HStoreLibrary.Delete.column[Map[String, String]](n, Node(c2)))
      }
    def --[P2, R](c2: Column[P2])(implicit om: o#arg[List[String], P2]#to[Map[String, String], R]) = {
        om(HStoreLibrary.Delete.column[Map[String, String]](n, Node(c2)))
      }
    def -/[P2, R](c2: Column[P2])(implicit om: o#arg[String, P2]#to[Map[String, String], R]) = {
        om(HStoreLibrary.Delete.column[Map[String, String]](n, Node(c2)))
      }
  }
}
