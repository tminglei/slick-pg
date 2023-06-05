package com.github.tminglei.slickpg

import cats.syntax.either._
import slick.jdbc.{GetResult, JdbcType, PositionedResult, PostgresProfile, SetParameter}

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
    import utils.JsonUtils.clean
    implicit val circeJsonTypeMapper: JdbcType[Json] =
      new GenericJdbcType[Json](
        pgjson,
        (v) => parse(v).getOrElse(Json.Null),
        (v) => clean(v.asJson.spaces2),
        hasLiteralForm = false
      )

    implicit def circeJsonColumnExtensionMethods(c: Rep[Json]): JsonColumnExtensionMethods[Json, Json] = {
        new JsonColumnExtensionMethods[Json, Json](c)
      }

    implicit def circeJsonOptionColumnExtensionMethods(c: Rep[Option[Json]]): JsonColumnExtensionMethods[Json, Option[Json]] = {
        new JsonColumnExtensionMethods[Json, Option[Json]](c)
      }
  }

  trait CirceJsonPlainImplicits extends CirceCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(Json.Null)
      def nextJsonOption() = r.nextStringOption().map(parse(_).getOrElse(Json.Null))
    }

    implicit val getJson: GetResult[Json] = mkGetResult(_.nextJson())
    implicit val getJsonOption: GetResult[Option[Json]] = mkGetResult(_.nextJsonOption())
    implicit val setJson: SetParameter[Json] = mkSetParameter[Json](pgjson, _.asJson.spaces2)
    implicit val setJsonOption: SetParameter[Option[Json]] = mkOptionSetParameter[Json](pgjson, _.asJson.spaces2)
  }
}
