package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{PositionedResult, JdbcType}

trait PgJson4sSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import org.json4s._

  type DOCType
  def pgjson: String

  val jsonMethods: JsonMethods[DOCType]

  /// alias
  trait JsonImplicits extends Json4sJsonImplicits

  trait Json4sJsonImplicits {
    implicit val json4sJsonTypeMapper =
      new GenericJdbcType[JValue](
        pgjson,
        (s) => jsonMethods.parse(s),
        (v) => jsonMethods.compact(jsonMethods.render(v)),
        hasLiteralForm = false
      )

    implicit def json4sJsonColumnExtensionMethods(c: Rep[JValue])(
      implicit tm: JdbcType[JValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JValue, JValue](c)
      }
    implicit def json4sJsonOptionColumnExtensionMethods(c: Rep[Option[JValue]])(
      implicit tm: JdbcType[JValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JValue, Option[JValue]](c)
      }
  }

  trait Json4sJsonPlainImplicits {
    import utils.PlainSQLUtils._

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(JNull)
      def nextJsonOption() = r.nextStringOption().map(jsonMethods.parse(_))
    }

    //////////////////////////////////////////////////////////
    implicit val getJson = mkGetResult(_.nextJson())
    implicit val getJsonOption = mkGetResult(_.nextJsonOption())
    implicit val setJson = mkSetParameter[JValue](pgjson, (v) => jsonMethods.compact(jsonMethods.render(v)))
    implicit val setJsonOption = mkOptionSetParameter[JValue](pgjson, (v) => jsonMethods.compact(jsonMethods.render(v)))
  }
}
