package com.github.tminglei.slickpg

import slick.jdbc.{GetResult, JdbcType, PositionedResult, PostgresProfile, SetParameter}

import scala.reflect.classTag

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
trait PgLTreeSupport extends ltree.PgLTreeExtensions with utils.PgCommonJdbcTypes with array.PgArrayJdbcTypes { driver: PostgresProfile =>
  import driver.api._

  trait SimpleLTreeCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("ltree", classTag[LTree])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("_ltree", classTag[List[LTree]])
    }
  }

  /// alias
  trait LTreeImplicits extends SimpleLTreeImplicits

  trait SimpleLTreeImplicits extends SimpleLTreeCodeGenSupport {
    implicit val simpleLTreeTypeMapper: JdbcType[LTree] =
      new GenericJdbcType[LTree]("ltree",
        (v) => LTree(v),
        (v) => v.toString,
        hasLiteralForm = false
      )
    implicit val simpleLTreeListTypeMapper: JdbcType[List[LTree]] =
      new AdvancedArrayJdbcType[LTree]("ltree",
        fromString = utils.SimpleArrayUtils.fromString(LTree.apply)(_).map(_.toList).orNull,
        mkString = utils.SimpleArrayUtils.mkString[LTree](_.toString)(_),
        hasLiteralForm = true
      ).to(_.toList)

    implicit def simpleLTreeColumnExtensionMethods(c: Rep[LTree]): LTreeColumnExtensionMethods[LTree, LTree] = {
        new LTreeColumnExtensionMethods[LTree, LTree](c)
      }
    implicit def simpleLTreeOptionColumnExtensionMethods(c: Rep[Option[LTree]]): LTreeColumnExtensionMethods[LTree, Option[LTree]] = {
        new LTreeColumnExtensionMethods[LTree, Option[LTree]](c)
      }

    implicit def simpleLTreeListColumnExtensionMethods(c: Rep[List[LTree]]): LTreeListColumnExtensionMethods[LTree, List[LTree]] = {
        new LTreeListColumnExtensionMethods[LTree, List[LTree]](c)
      }
    implicit def simpleLTreeListOptionColumnExtensionMethods(c: Rep[Option[List[LTree]]]): LTreeListColumnExtensionMethods[LTree, Option[List[LTree]]] = {
        new LTreeListColumnExtensionMethods[LTree, Option[List[LTree]]](c)
      }
  }

  trait SimpleLTreePlainImplicits extends SimpleLTreeCodeGenSupport {
    import utils.PlainSQLUtils._

    // to support 'nextArray[T]/nextArrayOption[T]' in PgArraySupport
    {
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(LTree.apply)(r.nextString()))
    }

    implicit class PgLTreePositionedResult(r: PositionedResult) {
      def nextLTree() = nextLTreeOption().orNull
      def nextLTreeOption() = r.nextStringOption().map(LTree.apply)
    }

    ///////////////////////////////////////////////////////////
    implicit val getLTree: GetResult[LTree] = mkGetResult(_.nextLTree())
    implicit val getLTreeOption: GetResult[Option[LTree]] = mkGetResult(_.nextLTreeOption())
    implicit val setLTree: SetParameter[LTree] = mkSetParameter[LTree]("ltree")
    implicit val setLTreeOption: SetParameter[Option[LTree]] = mkOptionSetParameter[LTree]("ltree")
    ///
    implicit val setLTreeArray: SetParameter[Seq[LTree]] = mkArraySetParameter[LTree]("ltree")
    implicit val setLTreeArrayOption: SetParameter[Option[Seq[LTree]]] = mkArrayOptionSetParameter[LTree]("ltree")
  }
}
