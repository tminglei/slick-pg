package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.{PositionedParameters, PositionedResult, JdbcType}
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
      new AdvancedArrayListJdbcType[LTree]("ltree",
        fromString = utils.SimpleArrayUtils.fromString(LTree.apply)(_).orNull,
        mkString = utils.SimpleArrayUtils.mkString[LTree](_.toString)(_)
      )

    implicit def simpleLTreeColumnExtensionMethods(c: Column[LTree])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeColumnExtensionMethods[LTree, LTree](c)
      }
    implicit def simpleLTreeOptionColumnExtensionMethods(c: Column[Option[LTree]])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeColumnExtensionMethods[LTree, Option[LTree]](c)
      }

    implicit def simpleLTreeListColumnExtensionMethods(c: Column[List[LTree]])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeListColumnExtensionMethods[LTree, List[LTree]](c)
      }
    implicit def simpleLTreeListOptionColumnExtensionMethods(c: Column[Option[List[LTree]]])(
      implicit tm: JdbcType[LTree], tm1: JdbcType[List[LTree]]) = {
        new LTreeListColumnExtensionMethods[LTree, Option[List[LTree]]](c)
      }
  }

  trait SimpleLTreePlainImplicits {
    import utils.SimpleArrayUtils._

    implicit class PgLTreePositionedResult(r: PositionedResult) {
      def nextLTree() = nextLTreeOption().orNull
      def nextLTreeOption() = r.nextStringOption().map(LTree.apply)
      def nextLTreeArray() = nextLTreeArrayOption().getOrElse(Nil)
      def nextLTreeArrayOption() = {
        val value = r.rs.getString(r.skip.currentPos)
        if (r.wasNull) None else Some(fromString(LTree.apply)(value))
      }
    }
    implicit class PgLTreePositionedParameters(p: PositionedParameters) {
      def setLTree(v: LTree) = setLTreeOption(Option(v))
      def setLTreeOption(v: Option[LTree]) = {
        p.pos += 1
        v match {
          case Some(v) => p.ps.setObject(p.pos, utils.mkPGobject("ltree", v.toString))
          case None    => p.ps.setNull(p.pos, java.sql.Types.OTHER)
        }
      }
      def setLTreeArray(v: List[LTree]) = setLTreeArrayOption(Option(v))
      def setLTreeArrayOption(v: Option[List[LTree]]) = {
        p.pos += 1
        v match {
          case Some(v) => p.ps.setArray(p.pos, mkArray(mkString[LTree](_.toString))("ltree", v))
          case None    => p.ps.setNull(p.pos, java.sql.Types.ARRAY)
        }
      }
    }
  }
}
