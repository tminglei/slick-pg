package com.github.tminglei.slickpg

import slick.jdbc.{GetResult, JdbcType, PositionedResult, PostgresProfile, SetParameter}

import scala.reflect.classTag

trait PgUPickleJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._

  ///---
  def pgjson: String
  ///---

  trait UPickleCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("json", classTag[ujson.Value])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("jsonb", classTag[ujson.Value])
    }
  }

  trait JsonImplicits extends UPickleImplicits

  trait UPickleImplicits extends UPickleCodeGenSupport {
    import utils.JsonUtils.clean
    implicit val uPickleJsonTypeMapper: JdbcType[ujson.Value] =
      new GenericJdbcType[ujson.Value](
        pgjson,
        v => ujson.read(v),
        v => clean(ujson.write(v)),
        hasLiteralForm = false
      )

    implicit def uPickleJsonColumnExtensionMethods(c: Rep[ujson.Value]): JsonColumnExtensionMethods[ujson.Value, ujson.Value] = {
        new JsonColumnExtensionMethods[ujson.Value, ujson.Value](c)
      }

    implicit def uPickleJsonOptionColumnExtensionMethods(c: Rep[Option[ujson.Value]]): JsonColumnExtensionMethods[ujson.Value, Option[ujson.Value]] = {
        new JsonColumnExtensionMethods[ujson.Value, Option[ujson.Value]](c)
      }
  }

  trait UPickleJsonPlainImplicits extends UPickleCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionResult(r: PositionedResult) {
      def nextJson(): ujson.Value = nextJsonOption().getOrElse(ujson.Null)
      def nextJsonOption(): Option[ujson.Value] = r.nextStringOption().map(ujson.read(_))
    }

    implicit val getJson: GetResult[ujson.Value] = mkGetResult(_.nextJson())
    implicit val getJsonOption: GetResult[Option[ujson.Value]] = mkGetResult(_.nextJsonOption())
    implicit val setJson: SetParameter[ujson.Value] = mkSetParameter[ujson.Value](pgjson, ujson.write(_))
    implicit val setJsonOption: SetParameter[Option[ujson.Value]] = mkOptionSetParameter[ujson.Value](pgjson, ujson.write(_))
  }
}
