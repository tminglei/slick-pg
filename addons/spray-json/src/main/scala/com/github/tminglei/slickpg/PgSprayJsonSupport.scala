package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column

trait PgSprayJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import spray.json._
  import DefaultJsonProtocol._ // !!! IMPORTANT, otherwise `convertTo` and `toJson` won't work correctly.

  type DOCType
  type JSONType = JsValue

  trait JsonImplicits {
    implicit val jsonTypeMapper =
      new GenericJdbcType[JsValue](
        "json",
        (v) => v.asJson,
        (v) => v.toJson.prettyPrint,
        hasLiteralForm = false
      )

    implicit def jsonColumnExtensionMethods(c: Column[JsValue])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
      new JsonColumnExtensionMethods[JsValue](c)
    }
    implicit def jsonOptionColumnExtensionMethods(c: Column[Option[JsValue]])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
      new JsonColumnExtensionMethods[Option[JsValue]](c)
    }
  }
}
