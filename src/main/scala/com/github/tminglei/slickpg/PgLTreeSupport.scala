package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.JdbcType
import scala.slick.lifted.Column

/** simple ltree wrapper */
case class LTree(value: List[String]) {
  override def toString = value.mkString(".")
}

object LTree {
  def apply(value: String): LTree = LTree(value.split("\\.").toList)
}

/**
 * simple ltree support; if all you want is just getting from / saving to db, and using pg json operations/methods, it should be enough
 */
trait PgLTreeSupport extends ltree.PgLTreeExtensions with utils.PgCommonJdbcTypes with array.PgArrayJdbcTypes { driver: PostgresDriver =>

  trait LTreeImplicits {
    implicit val ltreeTypeMapper =
      new GenericJdbcType[LTree]("ltree",
        (v) => LTree(v),
        (v) => v.toString,
        hasLiteralForm = false
      )
    implicit val ltreeListTypeMapper =
      new AdvancedArrayListJdbcType[LTree]("ltree",
        fromString = utils.SimpleArrayUtils.fromString(_)(LTree.apply).orNull,
        mkString = utils.SimpleArrayUtils.mkString(_)(_.toString)
      )

    implicit def ltreeColumnExtensionMethods(c: Column[LTree])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeColumnExtensionMethods[LTree, LTree](c)
      }
    implicit def ltreeOptionColumnExtensionMethods(c: Column[Option[LTree]])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeColumnExtensionMethods[LTree, Option[LTree]](c)
      }

    implicit def ltreeListColumnExtensionMethods(c: Column[List[LTree]])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeListColumnExtensionMethods[LTree, List[LTree]](c)
      }
    implicit def ltreeListOptionColumnExtensionMethods(c: Column[Option[List[LTree]]])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeListColumnExtensionMethods[LTree, Option[List[LTree]]](c)
      }
  }
}
