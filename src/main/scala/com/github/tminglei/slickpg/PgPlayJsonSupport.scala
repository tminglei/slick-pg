package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.{TypeMapper, Column}

trait PgPlayJsonSupport extends json.PgJsonExtensions { driver: PostgresDriver =>
  import play.api.libs.json._

  type JSONType = JsValue

  trait JsonImplicits {
    implicit val jsonTypeMapper =
      new utils.GenericTypeMapper[JsValue]("json",
        (v) => Json.parse(v),
        (v) => Json.stringify(v)
      )

    implicit def jsonColumnExtensionMethods(c: Column[JsValue])(
      implicit tm: TypeMapper[JsValue], tm1: TypeMapper[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue](c)
      }
    implicit def jsonOptionColumnExtensionMethods(c: Column[Option[JsValue]])(
      implicit tm: TypeMapper[JsValue], tm1: TypeMapper[List[String]]) = {
        new JsonColumnExtensionMethods[Option[JsValue]](c)
      }
  }
}