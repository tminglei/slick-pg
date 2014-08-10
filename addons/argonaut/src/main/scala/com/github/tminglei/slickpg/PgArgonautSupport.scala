package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.slick.jdbc.JdbcType

trait PgArgonautSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import argonaut._, Argonaut._

  trait JsonImplicits {
    implicit val jsonTypeMapper =
      new GenericJdbcType[Json](
        "json",
        (s) => s.parse.toOption.getOrElse(jNull),
        (v) => v.nospaces,
        hasLiteralForm = false
      )

    implicit def jsonColumnExtensionMethods(c: Column[Json])(
      implicit tm: JdbcType[Json], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[Json, Json](c)
      }
    implicit def jsonOptionColumnExtensionMethods(c: Column[Option[Json]])(
      implicit tm: JdbcType[Json], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[Json, Option[Json]](c)
      }
  }
}
