package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{PositionedResult, JdbcType}

/** simple json string wrapper */
case class JsonString(value: String)

/**
 * simple json support; if all you want is just getting from / saving to db, and using pg json operations/methods, it should be enough
 */
trait PgJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._

  def pgjson: String

  /// alias
  trait JsonImplicits extends SimpleJsonImplicits

  trait SimpleJsonImplicits {
    implicit val simpleJsonTypeMapper =
      new GenericJdbcType[JsonString](
        pgjson,
        (v) => JsonString(v),
        (v) => v.value,
        hasLiteralForm = false
      )

    implicit def simpleJsonColumnExtensionMethods(c: Rep[JsonString])(
      implicit tm: JdbcType[JsonString]) = {
        new JsonColumnExtensionMethods[JsonString, JsonString](c)
      }
    implicit def simpleJsonOptionColumnExtensionMethods(c: Rep[Option[JsonString]])(
      implicit tm: JdbcType[JsonString]) = {
        new JsonColumnExtensionMethods[JsonString, Option[JsonString]](c)
      }
  }

  trait SimpleJsonPlainImplicits {
    import utils.PlainSQLUtils._
    import scala.reflect.classTag

    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("json", classTag[JsonString])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("jsonb", classTag[JsonString])
    }

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().orNull
      def nextJsonOption() = r.nextStringOption().map(JsonString)
    }

    //////////////////////////////////////////////////////////////
    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[JsonString](pgjson, _.value)
    implicit val setJsonOption = mkOptionSetParameter[JsonString](pgjson, _.value)
  }
}
