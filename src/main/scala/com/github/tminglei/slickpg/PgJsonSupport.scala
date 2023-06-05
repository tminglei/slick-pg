package com.github.tminglei.slickpg

import com.github.tminglei.slickpg.utils.JsonUtils
import slick.jdbc.{GetResult, JdbcType, PositionedResult, PostgresProfile, SetParameter}

import scala.reflect.classTag

/** simple json string wrapper */
case class JsonString(value: String)

/**
 * simple json support; if all you want is just getting from / saving to db, and using pg json operations/methods, it should be enough
 */
trait PgJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._

  ///---
  def pgjson: String
  ///---

  trait SimpleJsonCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("json", classTag[JsonString])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("jsonb", classTag[JsonString])
    }
  }

  /// alias
  trait JsonImplicits extends SimpleJsonImplicits

  trait SimpleJsonImplicits extends SimpleJsonCodeGenSupport {
    implicit val simpleJsonTypeMapper: JdbcType[JsonString] =
      new GenericJdbcType[JsonString](
        pgjson,
        (v) => JsonString(v),
        (v) => JsonUtils.clean(v.value),
        hasLiteralForm = false
      )

    implicit def simpleJsonColumnExtensionMethods(c: Rep[JsonString]): JsonColumnExtensionMethods[JsonString, JsonString] = {
        new JsonColumnExtensionMethods[JsonString, JsonString](c)
      }
    implicit def simpleJsonOptionColumnExtensionMethods(c: Rep[Option[JsonString]]): JsonColumnExtensionMethods[JsonString, Option[JsonString]] = {
        new JsonColumnExtensionMethods[JsonString, Option[JsonString]](c)
      }
  }

  trait SimpleJsonPlainImplicits extends SimpleJsonCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().orNull
      def nextJsonOption() = r.nextStringOption().map(JsonString.apply)
    }

    //////////////////////////////////////////////////////////////
    implicit val getJson: GetResult[JsonString] = mkGetResult(_.nextJson())
    implicit val getJsonOption: GetResult[Option[JsonString]] = mkGetResult(_.nextJsonOption())
    implicit val setJson: SetParameter[JsonString] = mkSetParameter[JsonString](pgjson, _.value)
    implicit val setJsonOption: SetParameter[Option[JsonString]] = mkOptionSetParameter[JsonString](pgjson, _.value)
  }
}
