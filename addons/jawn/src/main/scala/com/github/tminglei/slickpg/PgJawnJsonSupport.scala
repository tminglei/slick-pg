package com.github.tminglei.slickpg

import slick.jdbc.{JdbcType, PositionedResult, PostgresProfile}
import scala.reflect.classTag

trait PgJawnJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresProfile =>
  import driver.api._
  import org.typelevel.jawn.ast._

  ///---
  def pgjson: String
  def u0000_pHolder = "[\\\\_u_0000]" //!!! change if if necessary
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
    implicit val playJsonTypeMapper: JdbcType[JValue] =
      new GenericJdbcType[JValue](
        pgjson,
        (v) => JParser.parseUnsafe(v),
        (v) => v.render
          .replace("\\\\u0000", u0000_pHolder)
          .replace("\\u0000", "")
          .replace(u0000_pHolder, "\\\\u0000"),
        hasLiteralForm = false
      )

    implicit def playJsonColumnExtensionMethods(c: Rep[JValue]) = {
        new JsonColumnExtensionMethods[JValue, JValue](c)
      }
    implicit def playJsonOptionColumnExtensionMethods(c: Rep[Option[JValue]]) = {
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
    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[JValue](pgjson, CanonicalRenderer.render)
    implicit val setJsonOption = mkOptionSetParameter[JValue](pgjson, CanonicalRenderer.render)
  }
}
