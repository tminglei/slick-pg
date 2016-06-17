package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{JdbcType, PositionedResult}

trait PgPlayJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import play.api.libs.json._

  def pgjson: String

  /// alias
  trait JsonImplicits extends PlayJsonImplicits

  trait PlayJsonImplicits {
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

  trait PlayJsonPlainImplicits {
    import utils.PlainSQLUtils._

    import scala.reflect.classTag

    // used to support code gen
    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("json", classTag[JsValue])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("jsonb", classTag[JsValue])
    }

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
