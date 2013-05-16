package com.github.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.{OptionMapperDSL, Column, TypeMapper}
import scala.slick.ast.{Library, LiteralNode, Node}
import scala.slick.ast.Library.{SqlFunction, SqlOperator}

trait PgSearchSupport { driver: PostgresDriver =>

  case class TsVector[P: TypeMapper](text: Column[P], shadow: Boolean = false) extends Column[P] {
    def nodeDelegate = if (shadow) text.nodeDelegate else SearchLibrary.ToTsVector.typed[P](Node(text))
  }
  case class TsQuery[P: TypeMapper](query: Column[P], shadow: Boolean = false) extends Column[P] {
    def nodeDelegate = if (shadow) query.nodeDelegate else SearchLibrary.ToTsQuery.typed[P](Node(query))
  }

  //----------------------------------------------------------------------

  trait SearchImplicits {
    implicit def TsVectorColumnExtensionMethods0(c: TsVector[String]) = new TsVectorColumnExtensionMethods(c)
    implicit def TsVectorOptionColumnExtensionMethods(c: TsVector[Option[String]]) = new TsVectorColumnExtensionMethods(c)
    implicit def TsQueryColumnExtensionMethods0(c: TsQuery[String]) = new TsQueryColumnExtensionMethods(c)
    implicit def TsQueryOptionColumnExtensionMethods(c: TsQuery[Option[String]]) = new TsQueryColumnExtensionMethods(c)
  }

  trait SearchAssistants {
    def tsVector[P: TypeMapper](text: Column[P]) = TsVector(text)
    def tsQuery[P: TypeMapper](query: Column[P]) = TsQuery(query)

    def tsPlainQuery[P: TypeMapper](query: Column[P]) = {
      TsQuery(SearchLibrary.PlainToTsQuery.column[P](Node(query)), shadow = true)
    }

    def tsHeadline[P1, P2, R](text: Column[P1], query: TsQuery[P2], config: Option[String] = None, options: Option[String] = None)(
      implicit om: OptionMapperDSL.arg[String, P1]#arg[String, P2]#to[String, R]) = (config, options) match {
        case (Some(conf), Some(opt)) => om(SearchLibrary.TsHeadline.column(LiteralNode(conf), Node(text), Node(query), LiteralNode(opt)))
        case (Some(conf), None) => om(SearchLibrary.TsHeadline.column(LiteralNode(conf), Node(text), Node(query)))
        case (None, Some(opt))  => om(SearchLibrary.TsHeadline.column(Node(text), Node(query), LiteralNode(opt)))
        case (None, None)       => om(SearchLibrary.TsHeadline.column(Node(text), Node(query)))
      }
    def tsRank[P1, P2, R](text: TsVector[P1], query: TsQuery[P2], weights: Option[List[Float]] = None, normalization: Option[Int] = None)(
      implicit om: OptionMapperDSL.arg[String, P1]#arg[String, P2]#to[Float, R], tm: TypeMapper[List[Float]]) = {
        val weightsNode = weights.map(w => Library.Cast.typed[List[Float]](LiteralNode(w)))
        (weights, normalization) match {
          case (Some(w), Some(n)) => om(SearchLibrary.TsRank.column(weightsNode.get, Node(text), Node(query), LiteralNode(n)))
          case (Some(w), None)  => om(SearchLibrary.TsRank.column(weightsNode.get, Node(text), Node(query)))
          case (None, Some(n))  => om(SearchLibrary.TsRank.column(Node(text), Node(query), LiteralNode(n)))
          case (None, None)     => om(SearchLibrary.TsRank.column(Node(text), Node(query)))
        }
      }
    def tsRankCD[P1, P2, R](text: TsVector[P1], query: TsQuery[P2], weights: Option[List[Float]] = None, normalization: Option[Int] = None)(
      implicit om: OptionMapperDSL.arg[String, P1]#arg[String, P2]#to[Float, R], tm: TypeMapper[List[Float]]) = {
        val weightsNode = weights.map(w => Library.Cast.typed[List[Float]](LiteralNode(w)))
        (weights, normalization) match {
          case (Some(w), Some(n)) => om(SearchLibrary.TsRankCD.column(weightsNode.get, Node(text), Node(query), LiteralNode(n)))
          case (Some(w), None)  => om(SearchLibrary.TsRankCD.column(weightsNode.get, Node(text), Node(query)))
          case (None, Some(n))  => om(SearchLibrary.TsRankCD.column(Node(text), Node(query), LiteralNode(n)))
          case (None, None)     => om(SearchLibrary.TsRankCD.column(Node(text), Node(query)))
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

  class TsVectorColumnExtensionMethods[P: TypeMapper](val c: TsVector[P]) {
    def @@(e: TsQuery[P]) = SearchLibrary.Matches.column[Boolean](Node(c), Node(e))
    def @+(e: TsVector[P]) = TsVector(SearchLibrary.Concatenate.column[P](Node(c), Node(e)), shadow = true)
  }

  class TsQueryColumnExtensionMethods[P: TypeMapper](val c: TsQuery[P]) {
    def @@(e: TsVector[P]) = SearchLibrary.Matches.column[Boolean](Node(c), Node(e))
    def @&(e: TsQuery[P]) = TsQuery(SearchLibrary.And.column[P](Node(c), Node(e)), shadow = true)
    def @|(e: TsQuery[P]) = TsQuery(SearchLibrary.Or.column[P](Node(c), Node(e)), shadow = true)
    def !! = TsQuery(SearchLibrary.Negate.column[P](Node(c)), shadow = true)
    def @>(e: TsQuery[P]) = SearchLibrary.Contains.column[Boolean](Node(c), Node(e))
  }
}
