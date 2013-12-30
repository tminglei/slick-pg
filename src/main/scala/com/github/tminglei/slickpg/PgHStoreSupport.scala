package com.github.tminglei.slickpg

import scala.collection.convert.{WrapAsJava, WrapAsScala}
import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import org.postgresql.util.HStoreConverter

trait PgHStoreSupport extends hstore.PgHStoreExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>

  trait HStoreImplicits {
    implicit val hstoreTypeMapper =
      new GenericJdbcType[Map[String, String]](
        "hstore",
        (v) => WrapAsScala.mapAsScalaMap(HStoreConverter.fromString(v).asInstanceOf[java.util.Map[String, String]]).toMap,
        (v) => HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v)),
        false
      )

    implicit def hstoreColumnExtensionMethods(c: Column[Map[String, String]])(
      implicit tm: JdbcType[Map[String, String]], tm1: JdbcType[List[String]]) = {
        new HStoreColumnExtensionMethods[Map[String, String]](c)
      }
    implicit def hstoreOptionColumnExtensionMethods(c: Column[Option[Map[String,String]]])(
      implicit tm: JdbcType[Map[String, String]], tm1: JdbcType[List[String]]) = {
        new HStoreColumnExtensionMethods[Option[Map[String, String]]](c)
      }
  }
}
