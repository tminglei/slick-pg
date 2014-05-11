package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.slick.jdbc.JdbcType

trait PgJson4sSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import org.json4s._

  type DOCType
  type JSONType = JValue

  val jsonMethods: JsonMethods[DOCType]

  trait JsonImplicits {
    implicit val jsonTypeMapper =
      new GenericJdbcType[JValue](
        "json",
        (s) => jsonMethods.parse(s),
        (v) => jsonMethods.compact(jsonMethods.render(v)),
        hasLiteralForm = false
      )

    implicit def jsonColumnExtensionMethods(c: Column[JValue])(
      implicit tm: JdbcType[JValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JValue](c)
      }
    implicit def jsonOptionColumnExtensionMethods(c: Column[Option[JValue]])(
      implicit tm: JdbcType[JValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[Option[JValue]](c)
      }
  }
}
