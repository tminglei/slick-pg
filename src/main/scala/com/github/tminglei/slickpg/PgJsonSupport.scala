package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.JdbcType
import scala.slick.lifted.Column

/** simple json string wrapper */
case class JsonString(value: String)

/**
 * simple json support; if all you want is just getting from / saving to db, and using pg json operations/methods, it should be enough
 */
trait PgJsonSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>

  trait JsonImplicits {
    implicit val jsonTypeMapper =
      new GenericJdbcType[JsonString]("json",
        (v) => JsonString(v),
        (v) => v.value,
        hasLiteralForm = false
      )

    implicit def jsonColumnExtensionMethods(c: Column[JsonString])(
      implicit tm: JdbcType[JsonString], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsonString, JsonString](c)
      }
    implicit def jsonOptionColumnExtensionMethods(c: Column[Option[JsonString]])(
      implicit tm: JdbcType[JsonString], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JsonString, Option[JsonString]](c)
      }
  }
}
