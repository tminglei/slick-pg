package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{PositionedResult, JdbcType}

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
  import driver.api._

  /// alias
  trait LTreeImplicits extends SimpleLTreeImplicits

  trait SimpleLTreeImplicits {
    implicit val simpleLTreeTypeMapper =
      new GenericJdbcType[LTree]("ltree",
        (v) => LTree(v),
        (v) => v.toString,
        hasLiteralForm = false
      )
    implicit val simpleLTreeListTypeMapper =
      new AdvancedArrayJdbcType[LTree]("ltree",
        fromString = utils.SimpleArrayUtils.fromString(LTree.apply)(_).map(_.toList).orNull,
        mkString = utils.SimpleArrayUtils.mkString[LTree](_.toString)(_)
      ).to(_.toList)

    implicit def simpleLTreeColumnExtensionMethods(c: Rep[LTree])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeColumnExtensionMethods[LTree, LTree](c)
      }
    implicit def simpleLTreeOptionColumnExtensionMethods(c: Rep[Option[LTree]])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeColumnExtensionMethods[LTree, Option[LTree]](c)
      }

    implicit def simpleLTreeListColumnExtensionMethods(c: Rep[List[LTree]])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeListColumnExtensionMethods[LTree, List[LTree]](c)
      }
    implicit def simpleLTreeListOptionColumnExtensionMethods(c: Rep[Option[List[LTree]]])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeListColumnExtensionMethods[LTree, Option[List[LTree]]](c)
      }
  }

  trait SimpleLTreePlainImplicits {
    import utils.PlainSQLUtils._
    import scala.reflect.classTag

    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("ltree", classTag[LTree])
    }

    if (driver.isInstanceOf[PgArraySupport]) {
      driver.asInstanceOf[PgArraySupport].addNextArrayConverter(
        _.nextStringOption().flatMap(utils.SimpleArrayUtils.fromString(LTree.apply)))
    }

    implicit class PgLTreePositionedResult(r: PositionedResult) {
      def nextLTree() = nextLTreeOption().orNull
      def nextLTreeOption() = r.nextStringOption().map(LTree.apply)
    }

    ///////////////////////////////////////////////////////////
    implicit val setLTree = mkSetParameter[LTree]("ltree")
    implicit val setLTreeOption = mkOptionSetParameter[LTree]("ltree")
    ///
    implicit val setLTreeArray = mkArraySetParameter[LTree]("ltree")
    implicit val setLTreeArrayOption = mkArrayOptionSetParameter[LTree]("ltree")
  }
}
