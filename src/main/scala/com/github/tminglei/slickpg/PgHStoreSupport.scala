package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.{TypeMapper, Column}
import org.postgresql.util.HStoreConverter
import scala.collection.convert.{WrapAsJava, WrapAsScala}

trait PgHStoreSupport extends hstore.PgHStoreExtensions { driver: PostgresDriver =>

  trait HStoreImplicits {
    implicit val hstoreTypeMapper =
      new utils.GenericTypeMapper[Map[String, String]](
        "hstore",
        (v) => WrapAsScala.mapAsScalaMap(HStoreConverter.fromString(v).asInstanceOf[java.util.Map[String, String]]).toMap,
        (v) => HStoreConverter.toString(WrapAsJava.mapAsJavaMap(v))
      )

    implicit def hstoreColumnExtensionMethods(c: Column[Map[String, String]])(
      implicit tm: TypeMapper[Map[String, String]], tm1: TypeMapper[List[String]]) = {
        new HStoreColumnExtensionMethods[Map[String, String]](c)
      }
    implicit def hstoreOptionColumnExtensionMethods(c: Column[Option[Map[String,String]]])(
      implicit tm: TypeMapper[Map[String, String]], tm1: TypeMapper[List[String]]) = {
        new HStoreColumnExtensionMethods[Option[Map[String, String]]](c)
      }
  }
}
