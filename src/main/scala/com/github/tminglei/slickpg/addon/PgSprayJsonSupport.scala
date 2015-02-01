package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}

trait PgSprayJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import spray.json._
  import DefaultJsonProtocol._ // !!! IMPORTANT, otherwise `convertTo` and `toJson` won't work correctly.

  val pgjson = "json"

  /// alias
  trait JsonImplicits extends SparyJsonImplicits

  trait SparyJsonImplicits {
    implicit val sparyJsonTypeMapper =
      new GenericJdbcType[JsValue](
        pgjson,
        (s) => s.parseJson,
        (v) => v.toJson.compactPrint,
        hasLiteralForm = false
      )

    implicit def sparyJsonColumnExtensionMethods(c: Column[JsValue])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue, JsValue](c)
      }
    implicit def sparyJsonOptionColumnExtensionMethods(c: Column[Option[JsValue]])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue, Option[JsValue]](c)
      }
  }

  trait SprayJsonPlainImplicits {

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(JsNull)
      def nextJsonOption() = r.nextStringOption().map(_.parseJson)
    }

    implicit object SetJson extends SetParameter[JsValue] {
      def apply(v: JsValue, pp: PositionedParameters) = setJson(Option(v), pp)
    }
    implicit object SetJsonOption extends SetParameter[Option[JsValue]] {
      def apply(v: Option[JsValue], pp: PositionedParameters) = setJson(v, pp)
    }

    ///
    private def setJson(v: Option[JsValue], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject("json", v.toJson.compactPrint), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
  }
}
