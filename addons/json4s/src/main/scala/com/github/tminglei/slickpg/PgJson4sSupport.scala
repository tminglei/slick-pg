package com.github.tminglei.slickpg

import slick.jdbc.{GetResult, JdbcType, PositionedResult, PostgresProfile, SetParameter}

import scala.reflect.classTag

trait PgJson4sSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._
  import org.json4s._

  ///---
  type DOCType
  def pgjson: String

  val jsonMethods: JsonMethods[DOCType]
  ///---

  trait Json4sCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("json", classTag[JValue])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("jsonb", classTag[JValue])
    }
  }

  /// alias
  trait JsonImplicits extends Json4sJsonImplicits

  trait Json4sJsonImplicits extends Json4sCodeGenSupport {
    import utils.JsonUtils.clean
    implicit val json4sJsonTypeMapper: JdbcType[JValue] =
      new GenericJdbcType[JValue](
        pgjson,
        (s) => jsonMethods.parse(s),
        (v) => clean(jsonMethods.compact(jsonMethods.render(v))),
        hasLiteralForm = false
      )

    implicit def json4sJsonColumnExtensionMethods(c: Rep[JValue]): JsonColumnExtensionMethods[JValue, JValue] = {
        new JsonColumnExtensionMethods[JValue, JValue](c)
      }
    implicit def json4sJsonOptionColumnExtensionMethods(c: Rep[Option[JValue]]): JsonColumnExtensionMethods[JValue, Option[JValue]] = {
        new JsonColumnExtensionMethods[JValue, Option[JValue]](c)
      }
  }

  trait Json4sJsonPlainImplicits extends Json4sCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(JNull)
      def nextJsonOption() = r.nextStringOption().map(jsonMethods.parse(_))
    }

    //////////////////////////////////////////////////////////
    implicit val getJson: GetResult[JValue] = mkGetResult(_.nextJson())
    implicit val getJsonOption: GetResult[Option[JValue]] = mkGetResult(_.nextJsonOption())
    implicit val setJson: SetParameter[JValue] = mkSetParameter[JValue](pgjson, (v) => jsonMethods.compact(jsonMethods.render(v)))
    implicit val setJsonOption: SetParameter[Option[JValue]] = mkOptionSetParameter[JValue](pgjson, (v) => jsonMethods.compact(jsonMethods.render(v)))
  }
}
