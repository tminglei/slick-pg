package com.github.tminglei.slickpg

import slick.jdbc.{GetResult, JdbcType, PositionedResult, PostgresProfile, SetParameter}

import scala.reflect.classTag

trait PgJawnJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._
  import org.typelevel.jawn.ast._

  ///---
  def pgjson: String
  ///---

  trait JawnJsonCodeGenSupport {
    // register types to let `ExModelBuilder` find them
    if (driver.isInstanceOf[ExPostgresProfile]) {
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("json", classTag[JValue])
      driver.asInstanceOf[ExPostgresProfile].bindPgTypeToScala("jsonb", classTag[JValue])
    }
  }

  /// alias
  trait JsonImplicits extends JawnJsonImplicits

  trait JawnJsonImplicits extends JawnJsonCodeGenSupport {
    import utils.JsonUtils.clean
    implicit val playJsonTypeMapper: JdbcType[JValue] =
      new GenericJdbcType[JValue](
        pgjson,
        v => JParser.parseUnsafe(v),
        v => clean(v.render()),
        hasLiteralForm = false
      )

    implicit def playJsonColumnExtensionMethods(c: Rep[JValue]): JsonColumnExtensionMethods[JValue, JValue] = {
        new JsonColumnExtensionMethods[JValue, JValue](c)
      }
    implicit def playJsonOptionColumnExtensionMethods(c: Rep[Option[JValue]]): JsonColumnExtensionMethods[JValue, Option[JValue]] = {
        new JsonColumnExtensionMethods[JValue, Option[JValue]](c)
      }
  }

  trait JawnJsonPlainImplicits extends JawnJsonCodeGenSupport {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson(): JValue = nextJsonOption().getOrElse(JNull)
      def nextJsonOption(): Option[JValue] = r.nextStringOption().map(JParser.parseUnsafe)
    }

    ////////////////////////////////////////////////////////////
    implicit val getJson: GetResult[JValue] = mkGetResult(_.nextJson())
    implicit val getJsonOption: GetResult[Option[JValue]] = mkGetResult(_.nextJsonOption())
    implicit val setJson: SetParameter[JValue] = mkSetParameter[JValue](pgjson, CanonicalRenderer.render)
    implicit val setJsonOption: SetParameter[Option[JValue]] = mkOptionSetParameter[JValue](pgjson, CanonicalRenderer.render)
  }
}
