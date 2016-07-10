package com.github.tminglei.slickpg

import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}
import scala.reflect.classTag

trait PgPlayJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._
  import play.api.libs.json._

  ///---
  def pgjson: String
  ///---

  trait PlayJsonCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("json", classTag[JsValue])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("jsonb", classTag[JsValue])
    }
  }

  /// alias
  trait JsonImplicits extends PlayJsonImplicits

  trait PlayJsonImplicits extends PlayJsonCodeGenSupport {
    implicit val playJsonTypeMapper: JdbcType[JsValue] =
      new GenericJdbcType[JsValue](
        pgjson,
        (v) => Json.parse(v),
        (v) => Json.stringify(v),
        zero = JsNull,
        hasLiteralForm = false
      )

    implicit def playJsonColumnExtensionMethods(c: Rep[JsValue]) = {
        new JsonColumnExtensionMethods[JsValue, JsValue](c)
      }
    implicit def playJsonOptionColumnExtensionMethods(c: Rep[Option[JsValue]]) = {
        new JsonColumnExtensionMethods[JsValue, Option[JsValue]](c)
      }
  }

  trait PlayJsonPlainImplicits extends PlayJsonCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(JsNull)
      def nextJsonOption() = r.nextStringOption().map(Json.parse)
    }

    ////////////////////////////////////////////////////////////
    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[JsValue](pgjson, Json.stringify)
    implicit val setJsonOption = mkOptionSetParameter[JsValue](pgjson, Json.stringify)
  }
}
