package com.github.tminglei.slickpg

import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}
import scala.reflect.classTag

trait PgSprayJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._
  import spray.json._
  import DefaultJsonProtocol._ // !!! IMPORTANT, otherwise `convertTo` and `toJson` won't work correctly.

  ///---
  def pgjson: String
  ///---

  trait SprayJsonCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("json", classTag[JsValue])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("jsonb", classTag[JsValue])
    }
  }

  /// alias
  trait JsonImplicits extends SprayJsonImplicits

  trait SprayJsonImplicits extends SprayJsonCodeGenSupport {
    implicit val sprayJsonTypeMapper: JdbcType[JsValue] =
      new GenericJdbcType[JsValue](
        pgjson,
        (s) => s.parseJson,
        (v) => v.toJson.compactPrint,
        zero = JsNull,
        hasLiteralForm = false
      )

    implicit def sprayJsonColumnExtensionMethods(c: Rep[JsValue]) = {
        new JsonColumnExtensionMethods[JsValue, JsValue](c)
      }
    implicit def sprayJsonOptionColumnExtensionMethods(c: Rep[Option[JsValue]]) = {
        new JsonColumnExtensionMethods[JsValue, Option[JsValue]](c)
      }
  }

  trait SprayJsonPlainImplicits extends SprayJsonCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(JsNull)
      def nextJsonOption() = r.nextStringOption().map(_.parseJson)
    }

    ///////////////////////////////////////////////////////////
    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[JsValue](pgjson, _.toJson.compactPrint)
    implicit val setJsonOption = mkOptionSetParameter[JsValue](pgjson, _.toJson.compactPrint)
  }
}
