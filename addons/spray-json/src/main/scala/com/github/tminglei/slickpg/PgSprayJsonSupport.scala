package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.slick.jdbc.JdbcType

trait PgSprayJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import spray.json._
  import DefaultJsonProtocol._ // !!! IMPORTANT, otherwise `convertTo` and `toJson` won't work correctly.

  /// alias
  trait JsonImplicits extends SparyJsonImplicits

  trait SparyJsonImplicits {
    implicit val sparyJsonTypeMapper =
      new GenericJdbcType[JsValue](
        "json",
        (s) => s.parseJson,
        (v) => v.toJson.compactPrint,
        hasLiteralForm = false
      )

    implicit def sparyJsonColumnExtensionMethods(c: Column[JsValue])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue, JsValue](c)
      }
    implicit def sparyJsonOptionColumnExtensionMethods(c: Column[Option[JsValue]])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue, Option[JsValue]](c)
      }
  }
}
