package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.slick.jdbc.{PositionedParameters, PositionedResult, JdbcType}

trait PgArgonautSupport extends json.PgJsonExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import argonaut._, Argonaut._

  /// alias
  trait JsonImplicits extends ArgonautJsonImplicits

  trait ArgonautJsonImplicits {
    implicit val argonautJsonTypeMapper =
      new GenericJdbcType[Json](
        "json",
        (s) => s.parse.toOption.getOrElse(jNull),
        (v) => v.nospaces,
        hasLiteralForm = false
      )

    implicit def argonautJsonColumnExtensionMethods(c: Column[Json])(
      implicit tm: JdbcType[Json], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[Json, Json](c)
      }
    implicit def argonautJsonOptionColumnExtensionMethods(c: Column[Option[Json]])(
      implicit tm: JdbcType[Json], tm1: JdbcType[List[String]]) = {
        new JsonColumnExtensionMethods[Json, Option[Json]](c)
      }
  }

  trait ArgonautJsonPlainImplicits {
    implicit class PgJsonPositionedResult(r: PositionedResult) {
      def nextJson() = nextJsonOption().getOrElse(jNull)
      def nextJsonOption() = r.nextStringOption().map(_.parse)
    }
    implicit class PgJsonPositionedParameters(p: PositionedParameters) {
      def setJson(v: Json) = setJsonOption(Option(v))
      def setJsonOption(v: Option[Json]) = {
        p.pos += 1
        v match {
          case Some(v) => p.ps.setObject(p.pos, utils.mkPGobject("json", v.nospaces))
          case None    => p.ps.setNull(p.pos, java.sql.Types.OTHER)
        }
      }
    }
  }
}
