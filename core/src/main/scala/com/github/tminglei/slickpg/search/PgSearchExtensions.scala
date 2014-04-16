package com.github.tminglei.slickpg
package search

import scala.slick.lifted.{FunctionSymbolExtensionMethods, OptionMapperDSL, Column}
import scala.slick.ast.{Library, LiteralNode}
import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.driver.{JdbcTypesComponent, PostgresDriver}
import scala.slick.jdbc.JdbcType

trait PgSearchExtensions extends JdbcTypesComponent { driver: PostgresDriver =>
  import driver.Implicit._
  import FunctionSymbolExtensionMethods._

  case class TsVector[P: JdbcType](text: Column[P], shadow: Boolean = false) extends Column[P] {
    def toNode = if (shadow) text.toNode else SearchLibrary.ToTsVector.column[P](text.toNode).toNode
  }
  case class TsQuery[P: JdbcType](query: Column[P], shadow: Boolean = false) extends Column[P] {
    def toNode = if (shadow) query.toNode else SearchLibrary.ToTsQuery.column[P](query.toNode).toNode
  }

  //----------------------------------------------------------------------

  trait SearchAssistants {
    def tsVector[P: JdbcType](text: Column[P]) = TsVector(text)
    def tsQuery[P: JdbcType](query: Column[P]) = TsQuery(query)

    def tsPlainQuery[P: JdbcType](query: Column[P]) = {
      TsQuery(SearchLibrary.PlainToTsQuery.column[P](query.toNode), shadow = true)
    }

    def tsHeadline[P1, P2, R](text: Column[P1], query: TsQuery[P2], config: Option[String] = None, options: Option[String] = None)(
                  implicit om: OptionMapperDSL.arg[String, P1]#arg[String, P2]#to[String, R]) =
      (config, options) match {
        case (Some(conf), Some(opt)) => om.column(SearchLibrary.TsHeadline, LiteralNode(conf), text.toNode, query.toNode, LiteralNode(opt))
        case (Some(conf), None) => om.column(SearchLibrary.TsHeadline, LiteralNode(conf), text.toNode, query.toNode)
        case (None, Some(opt))  => om.column(SearchLibrary.TsHeadline, text.toNode, query.toNode, LiteralNode(opt))
        case (None, None)       => om.column(SearchLibrary.TsHeadline, text.toNode, query.toNode)
      }
    def tsRank[P1, P2, R](text: TsVector[P1], query: TsQuery[P2], weights: Option[List[Float]] = None, normalization: Option[Int] = None)(
                  implicit om: OptionMapperDSL.arg[String, P1]#arg[String, P2]#to[Float, R], tm: JdbcType[List[Float]]) = {
      val weightsNode = weights.map(w => Library.Cast.column[List[Float]](LiteralNode(tm, w)).toNode)
      (weights, normalization) match {
        case (Some(w), Some(n)) => om.column(SearchLibrary.TsRank, weightsNode.get, text.toNode, query.toNode, LiteralNode(n))
        case (Some(w), None)  => om.column(SearchLibrary.TsRank, weightsNode.get, text.toNode, query.toNode)
        case (None, Some(n))  => om.column(SearchLibrary.TsRank, text.toNode, query.toNode, LiteralNode(n))
        case (None, None)     => om.column(SearchLibrary.TsRank, text.toNode, query.toNode)
      }
    }
    def tsRankCD[P1, P2, R](text: TsVector[P1], query: TsQuery[P2], weights: Option[List[Float]] = None, normalization: Option[Int] = None)(
                  implicit om: OptionMapperDSL.arg[String, P1]#arg[String, P2]#to[Float, R], tm: JdbcType[List[Float]]) = {
      val weightsNode = weights.map(w => Library.Cast.column[List[Float]](LiteralNode(tm, w)).toNode)
      (weights, normalization) match {
        case (Some(w), Some(n)) => om.column(SearchLibrary.TsRankCD, weightsNode.get, text.toNode, query.toNode, LiteralNode(n))
        case (Some(w), None)  => om.column(SearchLibrary.TsRankCD, weightsNode.get, text.toNode, query.toNode)
        case (None, Some(n))  => om.column(SearchLibrary.TsRankCD, text.toNode, query.toNode, LiteralNode(n))
        case (None, None)     => om.column(SearchLibrary.TsRankCD, text.toNode, query.toNode)
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

    val ToTsQuery = new SqlFunction("to_tsquery")
    val ToTsVector = new SqlFunction("to_tsvector")
    val PlainToTsQuery = new SqlFunction("plainto_tsquery")
    val TsHeadline = new SqlFunction("ts_headline")
    val TsRank = new SqlFunction("ts_rank")
    val TsRankCD = new SqlFunction("ts_rank_cd")
  }

  class TsVectorColumnExtensionMethods[P](val c: TsVector[P])(implicit tm: JdbcType[P]) {
    def @@(e: TsQuery[P]) = SearchLibrary.Matches.column[Boolean](c.toNode, e.toNode)
    def @+(e: TsVector[P]) = TsVector(SearchLibrary.Concatenate.column[P](c.toNode, e.toNode), shadow = true)
  }

  class TsQueryColumnExtensionMethods[P](val c: TsQuery[P])(implicit tm: JdbcType[P]) {
    def @@(e: TsVector[P]) = SearchLibrary.Matches.column[Boolean](c.toNode, e.toNode)
    def @&(e: TsQuery[P]) = TsQuery(SearchLibrary.And.column[P](c.toNode, e.toNode), shadow = true)
    def @|(e: TsQuery[P]) = TsQuery(SearchLibrary.Or.column[P](c.toNode, e.toNode), shadow = true)
    def !! = TsQuery(SearchLibrary.Negate.column[P](c.toNode), shadow = true)
    def @>(e: TsQuery[P]) = SearchLibrary.Contains.column[Boolean](c.toNode, e.toNode)
  }
}
