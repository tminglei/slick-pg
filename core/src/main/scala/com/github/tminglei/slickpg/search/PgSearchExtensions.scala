package com.github.tminglei.slickpg
package search

import scala.slick.lifted.{ExtensionMethods, FunctionSymbolExtensionMethods, OptionMapperDSL, Column}
import scala.slick.ast.{Library, LiteralNode}
import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.driver.{JdbcTypesComponent, PostgresDriver}
import scala.slick.jdbc.JdbcType

trait PgSearchExtensions extends JdbcTypesComponent { driver: PostgresDriver =>
  import driver.Implicit._
  import FunctionSymbolExtensionMethods._

  trait SearchAssistants[TV, TQ] {
    def currTsConfig() = SearchLibrary.GetCurrTsConfig.column[String]()

    def tsVector[P, R](text: Column[P])(implicit tm: JdbcType[P], tm1: JdbcType[TV], tm2: JdbcType[TQ],
      om: OptionMapperDSL.arg[String, P]#to[TV, R]) = {
        om.column(Library.Cast, text.toNode, LiteralNode("tsvector"))
      }
    def tsQuery[P, R](query: Column[P])(implicit tm: JdbcType[P], tm1: JdbcType[TV], tm2: JdbcType[TQ],
      om: OptionMapperDSL.arg[String, P]#to[TQ, R]) = {
        om.column(Library.Cast, query.toNode, LiteralNode("tsquery"))
      }
    def toTsVector[P, R](text: Column[P], config: Option[String] = None)(
        implicit tm: JdbcType[P], tm1: JdbcType[TV], tm2: JdbcType[TQ], om: OptionMapperDSL.arg[String, P]#to[TV, R]) =
      config match {
        case Some(conf) => om.column(SearchLibrary.ToTsVector, LiteralNode(conf), text.toNode)
        case None       => om.column(SearchLibrary.ToTsVector, text.toNode)
      }
    def toTsQuery[P, R](query: Column[P], config: Option[String] = None)(
        implicit tm: JdbcType[P], tm1: JdbcType[TV], tm2: JdbcType[TQ], om: OptionMapperDSL.arg[String, P]#to[TQ, R]) =
      config match {
        case Some(conf) => om.column(SearchLibrary.ToTsQuery, LiteralNode(conf), query.toNode)
        case None       => om.column(SearchLibrary.ToTsQuery, query.toNode)
      }
    def plainToTsQuery[P, R](query: Column[P], config: Option[String] = None)(
        implicit tm: JdbcType[P], tm1: JdbcType[TV], tm2: JdbcType[TQ], om: OptionMapperDSL.arg[String, P]#to[TQ, R]) =
      config match {
        case Some(conf) => om.column(SearchLibrary.PlainToTsQuery, LiteralNode(conf), query.toNode)
        case None       => om.column(SearchLibrary.PlainToTsQuery, query.toNode)
      }

    def tsHeadline[T, P, R](text: Column[T], query: Column[P], config: Option[String] = None, options: Option[String] = None)(
                  implicit om: OptionMapperDSL.arg[String, T]#arg[TQ, P]#to[String, R]) =
      (config, options) match {
        case (Some(conf), Some(opt)) => om.column(SearchLibrary.TsHeadline, LiteralNode(conf), text.toNode, query.toNode, LiteralNode(opt))
        case (Some(conf), None) => om.column(SearchLibrary.TsHeadline, LiteralNode(conf), text.toNode, query.toNode)
        case (None, Some(opt))  => om.column(SearchLibrary.TsHeadline, text.toNode, query.toNode, LiteralNode(opt))
        case (None, None)       => om.column(SearchLibrary.TsHeadline, text.toNode, query.toNode)
      }
    def tsRank[T, P](text: Column[T], query: Column[P], weights: Option[List[Float]] = None, normalization: Option[Int] = None)(
                  implicit tm: JdbcType[List[Float]]) = {
      val weightsNode = weights.map(w => Library.Cast.column[List[Float]](LiteralNode(tm, w)).toNode)
      (weights, normalization) match {
        case (Some(w), Some(n)) => SearchLibrary.TsRank.column[Float](weightsNode.get, text.toNode, query.toNode, LiteralNode(n))
        case (Some(w), None)    => SearchLibrary.TsRank.column[Float](weightsNode.get, text.toNode, query.toNode)
        case (None, Some(n))    => SearchLibrary.TsRank.column[Float](text.toNode, query.toNode, LiteralNode(n))
        case (None, None)       => SearchLibrary.TsRank.column[Float](text.toNode, query.toNode)
      }
    }
    def tsRankCD[T, P, R](text: Column[T], query: Column[P], weights: Option[List[Float]] = None, normalization: Option[Int] = None)(
                  implicit tm: JdbcType[List[Float]]) = {
      val weightsNode = weights.map(w => Library.Cast.column[List[Float]](LiteralNode(tm, w)).toNode)
      (weights, normalization) match {
        case (Some(w), Some(n)) => SearchLibrary.TsRankCD.column[Float](weightsNode.get, text.toNode, query.toNode, LiteralNode(n))
        case (Some(w), None)    => SearchLibrary.TsRankCD.column[Float](weightsNode.get, text.toNode, query.toNode)
        case (None, Some(n))    => SearchLibrary.TsRankCD.column[Float](text.toNode, query.toNode, LiteralNode(n))
        case (None, None)       => SearchLibrary.TsRankCD.column[Float](text.toNode, query.toNode)
      }
    }
  }

  //////////////////////////////////////////////////////////////////////////

  object SearchLibrary {
    val Matches = new SqlOperator("@@")
    val Concatenate = new SqlOperator("||")
    val And = new SqlOperator("&&")
    val Or = new SqlOperator("||")
    val Negate = new SqlOperator("!!")
    val Contains = new SqlOperator("@>")

    val GetCurrTsConfig = new SqlFunction("get_current_ts_config")
    val ToTsQuery = new SqlFunction("to_tsquery")
    val ToTsVector = new SqlFunction("to_tsvector")
    val PlainToTsQuery = new SqlFunction("plainto_tsquery")
    val TsHeadline = new SqlFunction("ts_headline")
    val TsRank = new SqlFunction("ts_rank")
    val TsRankCD = new SqlFunction("ts_rank_cd")
    val TsRewrite = new SqlFunction("ts_rewrite")
    val QueryTree = new SqlFunction("querytree")
    val SetWeight = new SqlFunction("setweight")
    val Strip = new SqlFunction("strip")
    val Length = new SqlFunction("length")
    val NumNode = new SqlFunction("numnode")
  }

  class TsVectorColumnExtensionMethods[TV, TQ, P1](val c: Column[P1])(
              implicit tm: JdbcType[TV], tm1: JdbcType[TQ]) extends ExtensionMethods[TV, P1] {
    def @@[P2, R](e: Column[P2])(implicit om: o#arg[TQ, P2]#to[Boolean, R]) =
      om.column(SearchLibrary.Matches, c.toNode, e.toNode)
    def @+[P2, R](e: Column[P2])(implicit om: o#arg[TV, P2]#to[TV, R]) =
      om.column(SearchLibrary.Concatenate, c.toNode, e.toNode)
    def length = SearchLibrary.Length.column[Int](c.toNode)
    def strip  = SearchLibrary.Strip.column[TV](c.toNode)
    def setWeight(w: Column[Char]) = SearchLibrary.SetWeight.column[TV](c.toNode, w.toNode)
  }

  class TsQueryColumnExtensionMethods[TV, TQ, P1](val c: Column[P1])(
              implicit tm: JdbcType[TV], tm1: JdbcType[TQ]) extends ExtensionMethods[TQ, P1] {
    def @@[P2, R](e: Column[P2])(implicit om: o#arg[TV, P2]#to[Boolean, R]) =
      om.column(SearchLibrary.Matches, c.toNode, e.toNode)
    def @&[P2, R](e: Column[P2])(implicit om: o#arg[TQ, P2]#to[TQ, R]) =
      om.column(SearchLibrary.And, c.toNode, e.toNode)
    def @|[P2, R](e: Column[P2])(implicit om: o#arg[TQ, P2]#to[TQ, R]) =
      om.column(SearchLibrary.Or, c.toNode, e.toNode)
    def !! = SearchLibrary.Negate.column[TQ](c.toNode)
    def @>[P2, R](e: Column[P2])(implicit om: o#arg[TQ, P2]#to[Boolean, R]) =
      om.column(SearchLibrary.Contains, c.toNode, e.toNode)

    def numNode = SearchLibrary.NumNode.column[Int](c.toNode)
    def queryTree = SearchLibrary.QueryTree.column[String](c.toNode)
    def rewrite(target: Column[TQ], substitute: Column[TQ]) = {
        SearchLibrary.TsRewrite.column[TQ](c.toNode, target.toNode, substitute.toNode)
      }
    def rewrite(select: Column[String]) = {
        SearchLibrary.TsRewrite.column[TQ](c.toNode, select.toNode)
      }
  }
}
