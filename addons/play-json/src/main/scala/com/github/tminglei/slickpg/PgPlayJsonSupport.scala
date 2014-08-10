package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.slick.jdbc.JdbcType

trait PgPlayJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import play.api.libs.json._

  trait JsonImplicits {
    implicit val jsonTypeMapper =
      new GenericJdbcType[JsValue]("json",
        (v) => Json.parse(v),
        (v) => Json.stringify(v),
        hasLiteralForm = false
      )

    implicit def jsonColumnExtensionMethods(c: Column[JsValue])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue, JsValue](c)
      }
    implicit def jsonOptionColumnExtensionMethods(c: Column[Option[JsValue]])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue, Option[JsValue]](c)
      }
  }
}