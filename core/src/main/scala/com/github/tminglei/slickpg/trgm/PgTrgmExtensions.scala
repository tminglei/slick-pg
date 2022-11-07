package com.github.tminglei.slickpg
package trgm

import slick.ast.Library.{SqlFunction, SqlOperator}
import slick.ast.TypedType
import slick.jdbc.{JdbcTypesComponent, PostgresProfile}
import slick.lifted.ExtensionMethods

/**
  * Created by minglei on 6/21/17.
  */
trait PgTrgmExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  import driver.api._

  object TrgmLibrary {
    val %   = new SqlOperator("%")
    val`<%` = new SqlOperator("<%")
    val %>  = new SqlOperator("%>")
    val <<% = new SqlOperator("<<%")
    val %>> = new SqlOperator("%>>")
    val <-> = new SqlOperator("<->")
    val <<-> = new SqlOperator("<<->")
    val <->> = new SqlOperator("<->>")
    val <<<-> = new SqlOperator("<<<->")
    val <->>> = new SqlOperator("<->>>")

    val similarity = new SqlFunction("similarity")
    val word_similarity = new SqlFunction("word_similarity")
    val strict_word_similarity = new SqlFunction("strict_word_similarity")
  }

  class PgTrgmColumnExtensionMethods[P1](val c: Rep[P1]) extends ExtensionMethods[String, P1] {
    protected implicit def b1Type = implicitly[TypedType[String]]

    def % [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(TrgmLibrary.%, n, e.toNode)
      }
    def `<%` [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(TrgmLibrary.`<%`, n, e.toNode)
      }
    def %> [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(TrgmLibrary.%>, n, e.toNode)
      }
    def <<% [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(TrgmLibrary.<<%, n, e.toNode)
      }
    def %>> [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(TrgmLibrary.%>>, n, e.toNode)
      }

    def <-> [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Double, R]) = {
        om.column(TrgmLibrary.<->, n, e.toNode)
      }
    def <<-> [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Double, R]) = {
        om.column(TrgmLibrary.<<->, n, e.toNode)
      }
    def <->> [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Double, R]) = {
        om.column(TrgmLibrary.<->>, n, e.toNode)
      }
    def <<<-> [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Double, R]) = {
        om.column(TrgmLibrary.<<<->, n, e.toNode)
      }
    def <->>> [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Double, R]) = {
        om.column(TrgmLibrary.<->>>, n, e.toNode)
      }

    def similarity [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Double, R]) = {
        om.column(TrgmLibrary.similarity, n, e.toNode)
      }
    def wordSimilarity [P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Double, R]) = {
        om.column(TrgmLibrary.word_similarity, n, e.toNode)
      }
    def strictWordSimilarity[P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Double, R]) = {
        om.column(TrgmLibrary.strict_word_similarity, n, e.toNode)
      }
  }
}
