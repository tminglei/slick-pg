package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}

trait PgPlayJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import play.api.libs.json._

  /// alias
  trait JsonImplicits extends PlayJsonImplicits

  trait PlayJsonImplicits {
    implicit val playJsonTypeMapper =
      new GenericJdbcType[JsValue]("json",
        (v) => Json.parse(v),
        (v) => Json.stringify(v),
        hasLiteralForm = false
      )

    implicit def playJsonColumnExtensionMethods(c: Column[JsValue])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue, JsValue](c)
      }
    implicit def playJsonOptionColumnExtensionMethods(c: Column[Option[JsValue]])(
      implicit tm: JdbcType[JsValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsValue, Option[JsValue]](c)
      }
  }

  trait PlayJsonPlainImplicits {

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(JsNull)
      def nextJsonOption() = r.nextStringOption().map(Json.parse)
    }

    implicit object SetJson extends SetParameter[JsValue] {
      def apply(v: JsValue, pp: PositionedParameters) = setJson(Option(v), pp)
    }
    implicit object SetJsonOption extends SetParameter[Option[JsValue]] {
      def apply(v: Option[JsValue], pp: PositionedParameters) = setJson(v, pp)
    }

    ///
    private def setJson(v: Option[JsValue], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject("json", Json.stringify(v)), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
  }
}