package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.slick.jdbc.JdbcType

trait PgJson4sSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import org.json4s._

  type DOCType

  val jsonMethods: JsonMethods[DOCType]

  /// alias
  trait JsonImplicits extends Json4sJsonImplicits

  trait Json4sJsonImplicits {
    implicit val json4sJsonTypeMapper =
      new GenericJdbcType[JValue](
        "json",
        (s) => jsonMethods.parse(s),
        (v) => jsonMethods.compact(jsonMethods.render(v)),
        hasLiteralForm = false
      )

    implicit def json4sJsonColumnExtensionMethods(c: Column[JValue])(
      implicit tm: JdbcType[JValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JValue, JValue](c)
      }
    implicit def json4sJsonOptionColumnExtensionMethods(c: Column[Option[JValue]])(
      implicit tm: JdbcType[JValue], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[JValue, Option[JValue]](c)
      }
  }
}
