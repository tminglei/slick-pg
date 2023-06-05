package com.github.tminglei.slickpg

import scala.jdk.CollectionConverters._
import scala.reflect.classTag
import slick.jdbc.{GetResult, JdbcType, PositionedResult, PostgresProfile, SetParameter}
import org.postgresql.util.HStoreConverter


trait PgHStoreSupport extends hstore.PgHStoreExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._

  trait HStoreCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("hstore", classTag[Map[String, String]])
    }
  }

  /// alias
  trait HStoreImplicits extends SimpleHStoreImplicits

  trait SimpleHStoreImplicits extends HStoreCodeGenSupport {
    implicit val simpleHStoreTypeMapper: JdbcType[Map[String, String]] =
      new GenericJdbcType[Map[String, String]](
        "hstore",
        (v) => HStoreConverter.fromString(v).asScala.toMap,
        (v) => HStoreConverter.toString(v.asJava),
        hasLiteralForm = false
      )

    implicit def simpleHStoreColumnExtensionMethods(c: Rep[Map[String, String]])(implicit tm: JdbcType[List[String]]): HStoreColumnExtensionMethods[Map[String, String]] = {
        new HStoreColumnExtensionMethods[Map[String, String]](c)
      }
    implicit def simpleHStoreOptionColumnExtensionMethods(c: Rep[Option[Map[String,String]]])(implicit tm: JdbcType[List[String]]): HStoreColumnExtensionMethods[Option[Map[String, String]]] = {
        new HStoreColumnExtensionMethods[Option[Map[String, String]]](c)
      }
  }

  /// static sql support, NOTE: no extension methods available for static sql usage
  trait SimpleHStorePlainImplicits extends HStoreCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgHStorePositionedResult(r: PositionedResult) {
      def nextHStore() = nextHStoreOption().getOrElse(Map.empty)
      def nextHStoreOption() = r.nextStringOption().map { v =>
        HStoreConverter.fromString(v).asInstanceOf[java.util.Map[String, String]].asScala.toMap
      }
    }

    ////////////////////////////////////////////////////////////////////////
    implicit val getHStore: GetResult[Map[String, String]] = mkGetResult(_.nextHStore())
    implicit val getHStoreOption: GetResult[Option[Map[String, String]]] = mkGetResult(_.nextHStoreOption())
    implicit val setHStore: SetParameter[Map[String, String]] = mkSetParameter[Map[String, String]]("hstore",
      (v) => HStoreConverter.toString(v.asJava))
    implicit val setHStoreOption: SetParameter[Option[Map[String, String]]] = mkOptionSetParameter[Map[String, String]]("hstore",
      (v) => HStoreConverter.toString(v.asJava))
  }
}
