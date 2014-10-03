package com.github.tminglei.slickpg

import scala.collection.convert.{WrapAsJava, WrapAsScala}
import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import org.postgresql.util.HStoreConverter
import scala.slick.jdbc.JdbcType

trait PgHStoreSupport extends hstore.PgHStoreExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>

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

    implicit def simpleHStoreColumnExtensionMethods(c: Column[Map[String, String]])(
      implicit tm: JdbcType[Map[String, String]], tm1: JdbcType[List[String]]) = {
        new HStoreColumnExtensionMethods[Map[String, String]](c)
      }
    implicit def simpleHStoreOptionColumnExtensionMethods(c: Column[Option[Map[String,String]]])(
      implicit tm: JdbcType[Map[String, String]], tm1: JdbcType[List[String]]) = {
        new HStoreColumnExtensionMethods[Option[Map[String, String]]](c)
      }
  }
}
