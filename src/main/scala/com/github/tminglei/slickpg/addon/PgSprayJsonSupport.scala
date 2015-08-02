package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{PositionedResult, JdbcType}

trait PgSprayJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import spray.json._
  import DefaultJsonProtocol._ // !!! IMPORTANT, otherwise `convertTo` and `toJson` won't work correctly.

  def pgjson: String

  /// alias
  trait JsonImplicits extends SparyJsonImplicits

  trait SparyJsonImplicits {
    implicit val sparyJsonTypeMapper =
      new GenericJdbcType[JsValue](
        pgjson,
        (s) => s.parseJson,
        (v) => v.toJson.compactPrint,
        hasLiteralForm = false
      )

    implicit def sparyJsonColumnExtensionMethods(c: Rep[JsValue])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue, JsValue](c)
      }
    implicit def sparyJsonOptionColumnExtensionMethods(c: Rep[Option[JsValue]])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue, Option[JsValue]](c)
      }
  }

  trait SprayJsonPlainImplicits {
    import utils.PlainSQLUtils._
    import scala.reflect.classTag

    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("json", classTag[JsValue])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("jsonb", classTag[JsValue])
    }

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(JsNull)
      def nextJsonOption() = r.nextStringOption().map(_.parseJson)
    }

    ///////////////////////////////////////////////////////////
    implicit val setJson = mkSetParameter[JsValue](pgjson, _.toJson.compactPrint)
    implicit val setJsonOption = mkOptionSetParameter[JsValue](pgjson, _.toJson.compactPrint)
  }
}
