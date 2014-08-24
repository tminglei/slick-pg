package com.github.tminglei.slickpg
package search

import scala.slick.lifted.{FunctionSymbolExtensionMethods, OptionMapperDSL, Column}
import scala.slick.ast.{TypedType, Node, Library, LiteralNode}
import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.driver.{JdbcTypesComponent, PostgresDriver}
import scala.slick.jdbc.JdbcType

trait PgSearchExtensions extends JdbcTypesComponent { driver: PostgresDriver =>
  import driver.Implicit._
  import FunctionSymbolExtensionMethods._

  case class TsVector(val toNode: Node) extends Column[String]
  case class TsQuery(val toNode: Node) extends Column[String]

  //----------------------------------------------------------------------

  trait SearchAssistants {
    def currTsConfig() = SearchLibrary.GetCurrTsConfig.column[String]()

    def tsVector[P, R](text: Column[P])(implicit tm: JdbcType[P],
      om: OptionMapperDSL.arg[String, P]#to[String, R]) = {
        TsVector(Library.Cast.typed(implicitly[TypedType[P]], text.toNode, LiteralNode("tsvector")))
      }
    def tsQuery[P, R](query: Column[P])(implicit tm: JdbcType[P],
      om: OptionMapperDSL.arg[String, P]#to[String, R]) = {
        TsQuery(Library.Cast.typed(implicitly[TypedType[P]], query.toNode, LiteralNode("tsquery")))
      }
    def toTsVector[P, R](text: Column[P], config: Option[String] = None)(implicit tm: JdbcType[P],
      om: OptionMapperDSL.arg[String, P]#to[String, R]) = config match {
        case Some(conf) => TsVector(SearchLibrary.ToTsVector.typed(implicitly[TypedType[P]], LiteralNode(conf), text.toNode))
        case None       => TsVector(SearchLibrary.ToTsVector.typed(implicitly[TypedType[P]], text.toNode))
      }
    def toTsQuery[P, R](query: Column[P], config: Option[String] = None)(implicit tm: JdbcType[P],
      om: OptionMapperDSL.arg[String, P]#to[String, R]) = config match {
        case Some(conf) => TsQuery(SearchLibrary.ToTsQuery.typed(implicitly[TypedType[P]], LiteralNode(conf), query.toNode))
        case None       => TsQuery(SearchLibrary.ToTsQuery.typed(implicitly[TypedType[P]], query.toNode))
      }
    def plainToTsQuery[P, R](query: Column[P], config: Option[String] = None)(implicit tm: JdbcType[P],
      om: OptionMapperDSL.arg[String, P]#to[String, R]) = config match {
        case Some(conf) => TsQuery(SearchLibrary.PlainToTsQuery.typed(implicitly[TypedType[P]], LiteralNode(conf), query.toNode))
        case None       => TsQuery(SearchLibrary.PlainToTsQuery.typed(implicitly[TypedType[P]], query.toNode))
      }

    def tsHeadline[P, R](text: Column[P], query: TsQuery, config: Option[String] = None, options: Option[String] = None)(
                  implicit om: OptionMapperDSL.arg[String, P]#to[String, R]) =
      (config, options) match {
        case (Some(conf), Some(opt)) => om.column(SearchLibrary.TsHeadline, LiteralNode(conf), text.toNode, query.toNode, LiteralNode(opt))
        case (Some(conf), None) => om.column(SearchLibrary.TsHeadline, LiteralNode(conf), text.toNode, query.toNode)
        case (None, Some(opt))  => om.column(SearchLibrary.TsHeadline, text.toNode, query.toNode, LiteralNode(opt))
        case (None, None)       => om.column(SearchLibrary.TsHeadline, text.toNode, query.toNode)
      }
    def tsRank(text: TsVector, query: TsQuery, weights: Option[List[Float]] = None, normalization: Option[Int] = None)(
                  implicit tm: JdbcType[List[Float]]) = {
      val weightsNode = weights.map(w => Library.Cast.column[List[Float]](LiteralNode(tm, w)).toNode)
      (weights, normalization) match {
        case (Some(w), Some(n)) => SearchLibrary.TsRank.column[Float](weightsNode.get, text.toNode, query.toNode, LiteralNode(n))
        case (Some(w), None)    => SearchLibrary.TsRank.column[Float](weightsNode.get, text.toNode, query.toNode)
        case (None, Some(n))    => SearchLibrary.TsRank.column[Float](text.toNode, query.toNode, LiteralNode(n))
        case (None, None)       => SearchLibrary.TsRank.column[Float](text.toNode, query.toNode)
      }
    }
    def tsRankCD[R](text: TsVector, query: TsQuery, weights: Option[List[Float]] = None, normalization: Option[Int] = None)(
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

  class TsVectorColumnExtensionMethods(val c: TsVector) {
    def @@(e: TsQuery) = SearchLibrary.Matches.column[Boolean](c.toNode, e.toNode)
    def @+(e: TsVector) = TsVector(SearchLibrary.Concatenate.typed(implicitly[TypedType[String]], c.toNode, e.toNode))
    def length = SearchLibrary.Length.column[Int](c.toNode)
    def strip  = TsVector(SearchLibrary.Strip.typed(implicitly[TypedType[String]], c.toNode))
    def setWeight(w: Column[Char]) = TsVector(SearchLibrary.SetWeight.typed(implicitly[TypedType[String]], c.toNode, w.toNode))
  }

  class TsQueryColumnExtensionMethods(val c: TsQuery) {
    def @@(e: TsVector) = SearchLibrary.Matches.column[Boolean](c.toNode, e.toNode)
    def @&(e: TsQuery) = TsQuery(SearchLibrary.And.typed(implicitly[TypedType[String]], c.toNode, e.toNode))
    def @|(e: TsQuery) = TsQuery(SearchLibrary.Or.typed(implicitly[TypedType[String]], c.toNode, e.toNode))
    def !! = TsQuery(SearchLibrary.Negate.typed(implicitly[TypedType[String]], c.toNode))
    def @>(e: TsQuery) = SearchLibrary.Contains.column[Boolean](c.toNode, e.toNode)

    def numNode = SearchLibrary.NumNode.column[Int](c.toNode)
    def queryTree = SearchLibrary.QueryTree.column[String](c.toNode)
    def rewrite(target: TsQuery, substitute: TsQuery) = {
        TsQuery(SearchLibrary.TsRewrite.typed(implicitly[TypedType[String]], c.toNode, target.toNode, substitute.toNode))
      }
    def rewrite(select: Column[String]) = {
        TsQuery(SearchLibrary.TsRewrite.typed(implicitly[TypedType[String]], c.toNode, select.toNode))
      }
  }
}
