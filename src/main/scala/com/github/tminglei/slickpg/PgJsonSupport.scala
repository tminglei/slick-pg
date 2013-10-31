package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.{TypeMapper, Column}

trait PgJsonSupport extends json.PgJsonExtensions { driver: PostgresDriver =>
  import org.json4s._

  type DOCType
  type JSONType = JValue

  val jsonMethods: JsonMethods[DOCType]

  trait JsonImplicits {
    implicit val jsonTypeMapper =
      new utils.GenericTypeMapper[JValue]("json",
        (v) => jsonMethods.parse(v),
        (v) => jsonMethods.compact(jsonMethods.render(v))
      )

    implicit def jsonColumnExtensionMethods(c: Column[JValue])(
      implicit tm: TypeMapper[JValue], tm1: TypeMapper[List[String]]) = {
        new JsonColumnExtensionMethods[JValue](c)
      }
    implicit def jsonOptionColumnExtensionMethods(c: Column[Option[JValue]])(
      implicit tm: TypeMapper[JValue], tm1: TypeMapper[List[String]]) = {
        new JsonColumnExtensionMethods[Option[JValue]](c)
      }
  }
}
