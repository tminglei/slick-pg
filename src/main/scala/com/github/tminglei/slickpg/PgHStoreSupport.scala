package com.github.tminglei.slickpg

import scala.collection.convert.{WrapAsJava, WrapAsScala}
import org.postgresql.util.HStoreConverter
import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}
import scala.reflect.classTag

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
        (v) => WrapAsScala.mapAsScalaMap(HStoreConverter.fromString(v).asInstanceOf[java.util.Map[String, String]]).toMap,
        (v) => HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v)),
        hasLiteralForm = false
      )

    implicit def simpleHStoreColumnExtensionMethods(c: Rep[Map[String, String]])(implicit tm: JdbcType[List[String]]) = {
        new HStoreColumnExtensionMethods[Map[String, String]](c)
      }
    implicit def simpleHStoreOptionColumnExtensionMethods(c: Rep[Option[Map[String,String]]])(implicit tm: JdbcType[List[String]]) = {
        new HStoreColumnExtensionMethods[Option[Map[String, String]]](c)
      }
  }

  /// static sql support, NOTE: no extension methods available for static sql usage
  trait SimpleHStorePlainImplicits extends HStoreCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgHStorePositionedResult(r: PositionedResult) {
      def nextHStore() = nextHStoreOption().getOrElse(Map.empty)
      def nextHStoreOption() = r.nextStringOption().map { v =>
        WrapAsScala.mapAsScalaMap(HStoreConverter.fromString(v).asInstanceOf[java.util.Map[String, String]]).toMap
      }
    }

    ////////////////////////////////////////////////////////////////////////
    implicit val getHStore = mkGetResult(_.nextHStore())
    implicit val getHStoreOption = mkGetResult(_.nextHStoreOption())
    implicit val setHStore = mkSetParameter[Map[String, String]]("hstore",
      (v) => HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v)))
    implicit val setHStoreOption = mkOptionSetParameter[Map[String, String]]("hstore",
      (v) => HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v)))
  }
}
