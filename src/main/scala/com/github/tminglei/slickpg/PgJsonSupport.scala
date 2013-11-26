package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.slick.jdbc.JdbcType

trait PgJsonSupport extends json.PgJsonExtensions { driver: PostgresDriver =>
  import org.json4s._

  type DOCType
  type JSONType = JValue

  val jsonMethods: JsonMethods[DOCType]

  trait JsonImplicits {
    implicit val jsonTypeMapper =
      new utils.GenericJdbcType[JValue]("json",
        (v) => jsonMethods.parse(v),
        (v) => jsonMethods.compact(jsonMethods.render(v))
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
