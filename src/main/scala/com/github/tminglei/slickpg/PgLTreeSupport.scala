package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}
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
      new AdvancedArrayJdbcType[LTree]("ltree",
        fromString = utils.SimpleArrayUtils.fromString(LTree.apply)(_).map(_.toList).orNull,
        mkString = utils.SimpleArrayUtils.mkString[LTree](_.toString)(_)
      ).to(_.toList)

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
      def nextLTreeArrayOption() = r.nextStringOption().map(fromString(LTree.apply))
    }

    ///////////////////////////////////////////////////////////
    implicit object SetLTree extends SetParameter[LTree] {
      def apply(v: LTree, pp: PositionedParameters) = setLTree(Option(v), pp)
    }
    implicit object SetLTreeOption extends SetParameter[Option[LTree]] {
      def apply(v: Option[LTree], pp: PositionedParameters) = setLTree(v, pp)
    }

    implicit object SetLTreeArray extends SetParameter[List[LTree]] {
      def apply(v: List[LTree], pp: PositionedParameters) = setLTreeArray(Option(v), pp)
    }
    implicit object SetLTreeArrayOption extends SetParameter[Option[List[LTree]]] {
      def apply(v: Option[List[LTree]], pp: PositionedParameters) = setLTreeArray(v, pp)
    }

    ///
    private def setLTree(v: Option[LTree], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject("ltree", v.toString), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
    private def setLTreeArray(v: Option[List[LTree]], p: PositionedParameters) = {
      p.pos += 1
      v match {
        case Some(v) => p.ps.setArray(p.pos, mkArray(mkString[LTree](_.toString))("ltree", v))
        case None    => p.ps.setNull(p.pos, java.sql.Types.ARRAY)
      }
    }
  }
}
