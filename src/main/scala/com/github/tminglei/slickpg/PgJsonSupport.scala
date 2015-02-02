package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.{SetParameter, PositionedParameters, PositionedResult, JdbcType}
import scala.slick.lifted.Column

/** simple json string wrapper */
case class JsonString(value: String)

/**
 * simple json support; if all you want is just getting from / saving to db, and using pg json operations/methods, it should be enough
 */
trait PgJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>

  val pgjson = "json"

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

    implicit def simpleJsonColumnExtensionMethods(c: Column[JsonString])(
      implicit tm: JdbcType[JsonString], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsonString, JsonString](c)
      }
    implicit def simpleJsonOptionColumnExtensionMethods(c: Column[Option[JsonString]])(
      implicit tm: JdbcType[JsonString], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsonString, Option[JsonString]](c)
      }
  }

  trait SimpleJsonPlainImplicits {

    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().orNull
      def nextJsonOption() = r.nextStringOption().map(JsonString)
    }

    ///////////////////////////////////////////////////
    implicit object SetJson extends SetParameter[JsonString] {
      def apply(v: JsonString, pp: PositionedParameters) = setJson(Option(v), pp)
    }
    implicit object SetJsonOption extends SetParameter[Option[JsonString]] {
      def apply(v: Option[JsonString], pp: PositionedParameters) = setJson(v, pp)
    }

    ///
    private def setJson(v: Option[JsonString], p: PositionedParameters) = v match {
      case Some(v) => p.setObject(utils.mkPGobject("json", v.value), java.sql.Types.OTHER)
      case None    => p.setNull(java.sql.Types.OTHER)
    }
  }
}
