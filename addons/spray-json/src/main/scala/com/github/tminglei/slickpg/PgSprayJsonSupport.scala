package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{JdbcType, PositionedResult}

trait PgSprayJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import spray.json._
  import DefaultJsonProtocol._ // !!! IMPORTANT, otherwise `convertTo` and `toJson` won't work correctly.

  def pgjson: String

  /// alias
  trait JsonImplicits extends SprayJsonImplicits

  trait SprayJsonImplicits {
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

  trait SprayJsonPlainImplicits {
    import utils.PlainSQLUtils._

    import scala.reflect.classTag

    // used to support code gen
    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("json", classTag[JsValue])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("jsonb", classTag[JsValue])
    }

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
