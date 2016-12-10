package com.github.tminglei.slickpg.ltree

import slick.ast.Library.{SqlFunction, SqlOperator}
import slick.ast.{Library, LiteralNode, TypedType}
import slick.jdbc.{JdbcType, JdbcTypesComponent, PostgresProfile}
import slick.lifted.{ExtensionMethods, FunctionSymbolExtensionMethods}

trait PgLTreeExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  import driver.api._
  import FunctionSymbolExtensionMethods._

  object LTreeLibrary {
    val @> = new SqlOperator("@>")
    val <@ = new SqlOperator("<@")
    val ~  = new SqlOperator("~")
    val ?  = new SqlOperator("??")
    val @@ = new SqlOperator("@")
    val || = new SqlOperator("||")
    val ?@> = new SqlOperator("??@>")
    val ?<@ = new SqlOperator("??<@")
    val ?~ = new SqlOperator("??~")
    val ?@ = new SqlOperator("??@")

    val subltree = new SqlFunction("subltree")
    val subpath = new SqlFunction("subpath")
    val nlevel = new SqlFunction("nlevel")
    val index = new SqlFunction("index")
    val lca = new SqlFunction("lca")
  }

  class LTreeColumnExtensionMethods[B0, P1](val c: Rep[P1])(
              implicit tm: JdbcType[B0], tm1: JdbcType[List[B0]]) extends ExtensionMethods[B0, P1] {

    protected implicit def b1Type: TypedType[B0] = implicitly[TypedType[B0]]

    def @>[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om.column(LTreeLibrary.@>, n, e.toNode)
      }
    def <@:[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om.column(LTreeLibrary.<@, e.toNode, n)
      }
    def ~ [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        val lquery = Library.Cast.column[String](e.toNode, LiteralNode("lquery")).toNode
        om.column(LTreeLibrary.~, n, lquery)
      }
    def ~|[P2, R](e: Rep[P2])(implicit om: o#arg[List[String], P2]#to[Boolean, R], tm2: JdbcType[List[String]]) = {
        val lqueryArray = Library.Cast.column[List[String]](e.toNode, LiteralNode("lquery[]")).toNode
        om.column(LTreeLibrary.?, n, lqueryArray)
      }
    def @@[P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        val ltxtquery = Library.Cast.column[String](e.toNode, LiteralNode("ltxtquery")).toNode
        om.column(LTreeLibrary.@@, n, ltxtquery)
      }
    def ||[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[B0, R]) = {
        om.column(LTreeLibrary.||, n, e.toNode)
      }
    def || (e: Rep[String]) = LTreeLibrary.||.column[P1](n, e.toNode)
    def ||:(e: Rep[String]) = LTreeLibrary.||.column[P1](e.toNode, n)

    def subltree(start: Int, end: Int) = {
        LTreeLibrary.subltree.column[P1](n, LiteralNode(start), LiteralNode(end))
      }
    def subpath(offset: Int, length: Option[Int] = None) = length match {
      case Some(len) => LTreeLibrary.subpath.column[P1](n, LiteralNode(offset), LiteralNode(len))
      case None => LTreeLibrary.subpath.column[P1](n, LiteralNode(offset))
    }
    def nlevel() = LTreeLibrary.nlevel.column[Int](n)
    def index[P2, R](e: Rep[P2], offset: Option[Int] = None)(implicit om: o#arg[B0, P2]#to[Int, R]) = offset match {
      case Some(offsetN) => om.column(LTreeLibrary.index, n, e.toNode, LiteralNode(offsetN))
      case None => om.column(LTreeLibrary.index, n, e.toNode)
    }
  }

  class LTreeListColumnExtensionMethods[B0, P1](val c: Rep[P1])(
                  implicit tm: JdbcType[B0], tm1: JdbcType[List[B0]]) extends ExtensionMethods[List[B0], P1] {

    protected implicit def b1Type: TypedType[List[B0]] = implicitly[TypedType[List[B0]]]

    def @>[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om.column(LTreeLibrary.@>, n, e.toNode)
      }
    def <@:[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[Boolean, R]) = {
        om.column(LTreeLibrary.<@, e.toNode, n)
      }
    def ~ [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        val lquery = Library.Cast.column[String](e.toNode, LiteralNode("lquery")).toNode
        om.column(LTreeLibrary.~, n, lquery)
      }
    def ~|[P2, R](e: Rep[P2])(implicit om: o#arg[List[String], P2]#to[Boolean, R], tm2: JdbcType[List[String]]) = {
        val lqueryArray = Library.Cast.column[List[String]](e.toNode, LiteralNode("lquery[]")).toNode
        om.column(LTreeLibrary.?, n, lqueryArray)
      }
    def @@[P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        val ltxtquery = Library.Cast.column[String](e.toNode, LiteralNode("ltxtquery")).toNode
        om.column(LTreeLibrary.@@, n, ltxtquery)
      }
    def ?@>[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[B0, R]) = {
        om.column(LTreeLibrary.?@>, n, e.toNode)
      }
    def ?<@[P2, R](e: Rep[P2])(implicit om: o#arg[B0, P2]#to[B0, R]) = {
        om.column(LTreeLibrary.?<@, n, e.toNode)
      }
    def ?~[P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[B0, R]) = {
        val lquery = Library.Cast.column[String](e.toNode, LiteralNode("lquery")).toNode
        om.column(LTreeLibrary.?~, n, lquery)
      }
    def ?@[P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[B0, R]) = {
        val ltxtquery = Library.Cast.column[String](e.toNode, LiteralNode("ltxtquery")).toNode
        om.column(LTreeLibrary.?@, n, ltxtquery)
      }

    def lca[R](implicit om: o#to[B0, R]) = om.column(LTreeLibrary.lca, n)
  }
}
