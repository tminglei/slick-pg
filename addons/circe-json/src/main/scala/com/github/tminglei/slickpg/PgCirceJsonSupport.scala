package com.github.tminglei.slickpg

import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}
import scala.reflect.classTag

trait PgCirceJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._
  import io.circe._
  import io.circe.parser._
  import io.circe.syntax._

  ///---
  def pgjson: String
  ///---

  trait CirceCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("json", classTag[Json])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("jsonb", classTag[Json])
    }
  }

  trait JsonImplicits extends CirceImplicits

  trait CirceImplicits extends CirceCodeGenSupport {
    implicit val circeJsonTypeMapper: JdbcType[Json] =
      new GenericJdbcType[Json](
        pgjson,
        (v) => parse(v).getOrElse(Json.Null),
        (v) => v.asJson.spaces2,
        zero = Json.Null,
        hasLiteralForm = false
      )

    implicit def circeJsonColumnExtensionMethods(c: Rep[Json]) = {
        new JsonColumnExtensionMethods[Json, Json](c)
      }

    implicit def circeJsonOptionColumnExtensionMethods(c: Rep[Option[Json]]) = {
        new JsonColumnExtensionMethods[Json, Option[Json]](c)
      }
  }

  trait CirceJsonPlainImplicits extends CirceCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(Json.Null)
      def nextJsonOption() = r.nextStringOption().map(parse(_).getOrElse(Json.Null))
    }

    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[Json](pgjson, _.asJson.spaces2)
    implicit val setJsonOption = mkOptionSetParameter[Json](pgjson, _.asJson.spaces2)
  }
}
