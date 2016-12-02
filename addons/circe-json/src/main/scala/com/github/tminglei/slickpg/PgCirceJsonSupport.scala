package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{JdbcType, PositionedResult}

trait PgCirceJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import io.circe._
  import io.circe.generic.auto._
  import io.circe.parser._
  import io.circe.syntax._

  def pgjson: String

  trait JsonImplicits extends CirceImplicits

  trait CirceImplicits {
    implicit val circeJsonTypeMapper: JdbcType[Json] =
      new GenericJdbcType[Json](
        pgjson,
        (v) => parse(v).right.getOrElse(Json.Null),
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

  trait CirceJsonPlainImplicits {
    import utils.PlainSQLUtils._

    import scala.reflect.classTag

    // used to support code gen
    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("json", classTag[Json])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("jsonb", classTag[Json])
    }

    implicit class PgJsonPositionResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(Json.Null)
      def nextJsonOption() = r.nextStringOption().map(parse(_).right.getOrElse(Json.Null))
    }

    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[Json](pgjson, _.asJson.spaces2)
    implicit val setJsonOption = mkOptionSetParameter[Json](pgjson, _.asJson.spaces2)
  }
}
