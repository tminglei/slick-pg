package com.github.tminglei.slickpg

import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}
import scala.reflect.classTag

trait PgArgonautSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._
  import argonaut._, Argonaut._

  ///---
  def pgjson: String
  def u0000_pHolder = "[\\\\_u_0000]" //!!! change if if necessary
  ///---

  trait ArgonautCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("json", classTag[Json])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("jsonb", classTag[Json])
    }
  }

  /// alias
  trait JsonImplicits extends ArgonautJsonImplicits

  trait ArgonautJsonImplicits extends ArgonautCodeGenSupport {
    implicit val argonautJsonTypeMapper: JdbcType[Json] =
      new GenericJdbcType[Json](
        pgjson,
        (s) => s.parseOption.getOrElse(jNull),
        (v) => v.nospaces
          .replace("""\\u0000""", u0000_pHolder)
          .replace("\\u0000", "")
          .replace(u0000_pHolder, """\\u0000"""),
        hasLiteralForm = false
      )

    implicit def argonautJsonColumnExtensionMethods(c: Rep[Json]) = {
        new JsonColumnExtensionMethods[Json, Json](c)
      }
    implicit def argonautJsonOptionColumnExtensionMethods(c: Rep[Option[Json]]) = {
        new JsonColumnExtensionMethods[Json, Option[Json]](c)
      }
  }

  trait ArgonautJsonPlainImplicits extends ArgonautCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(jNull)
      def nextJsonOption() = r.nextStringOption().flatMap(_.parseOption)
    }

    ///////////////////////////////////////////////////////////
    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[Json](pgjson, _.nospaces)
    implicit val setJsonOption = mkOptionSetParameter[Json](pgjson, _.nospaces)
  }
}
