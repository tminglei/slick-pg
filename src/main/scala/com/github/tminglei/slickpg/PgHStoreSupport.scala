package com.github.tminglei.slickpg

import scala.collection.convert.{WrapAsJava, WrapAsScala}
import slick.driver.PostgresDriver
import org.postgresql.util.HStoreConverter
import slick.jdbc.{PositionedResult, JdbcType}

trait PgHStoreSupport extends hstore.PgHStoreExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._

  /// alias
  trait HStoreImplicits extends SimpleHStoreImplicits

  trait SimpleHStoreImplicits {
    implicit val simpleHStoreTypeMapper =
      new GenericJdbcType[Map[String, String]](
        "hstore",
        (v) => WrapAsScala.mapAsScalaMap(HStoreConverter.fromString(v).asInstanceOf[java.util.Map[String, String]]).toMap,
        (v) => HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v)),
        hasLiteralForm = false
      )

    implicit def simpleHStoreColumnExtensionMethods(c: Rep[Map[String, String]])(
      implicit tm: JdbcType[Map[String, String]], tm1: JdbcType[List[String]]) = {
        new HStoreColumnExtensionMethods[Map[String, String]](c)
      }
    implicit def simpleHStoreOptionColumnExtensionMethods(c: Rep[Option[Map[String,String]]])(
      implicit tm: JdbcType[Map[String, String]], tm1: JdbcType[List[String]]) = {
        new HStoreColumnExtensionMethods[Option[Map[String, String]]](c)
      }
  }

  /// static sql support, NOTE: no extension methods available for static sql usage
  trait SimpleHStorePlainImplicits {
    import utils.PlainSQLUtils._
    import scala.reflect.classTag

    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("hstore", classTag[Map[String, String]])
    }

    implicit class PgHStorePositionedResult(r: PositionedResult) {
      def nextHStore() = nextHStoreOption().getOrElse(Map.empty)
      def nextHStoreOption() = r.nextStringOption().map { v =>
        WrapAsScala.mapAsScalaMap(HStoreConverter.fromString(v).asInstanceOf[java.util.Map[String, String]]).toMap
      }
    }

    ////////////////////////////////////////////////////////////////////////
    implicit val setHStore = mkSetParameter[Map[String, String]]("hstore",
      (v) => HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v)))
    implicit val setHStoreOption = mkOptionSetParameter[Map[String, String]]("hstore",
      (v) => HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v)))
  }
}
