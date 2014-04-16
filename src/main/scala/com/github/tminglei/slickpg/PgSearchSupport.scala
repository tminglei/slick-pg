package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.JdbcType

trait PgSearchSupport extends search.PgSearchExtensions { driver: PostgresDriver =>
  import driver.Implicit._

  trait SearchImplicits {
    implicit def TsVectorColumnExtensionMethods0(c: TsVector[String]) = {
      new TsVectorColumnExtensionMethods(c)
    }
    implicit def TsVectorOptionColumnExtensionMethods(c: TsVector[Option[String]])(
      implicit tm: JdbcType[Option[String]]) = {
        new TsVectorColumnExtensionMethods(c)
      }
    implicit def TsQueryColumnExtensionMethods0(c: TsQuery[String]) = {
      new TsQueryColumnExtensionMethods(c)
    }
    implicit def TsQueryOptionColumnExtensionMethods(c: TsQuery[Option[String]])(
      implicit tm: JdbcType[Option[String]]) = {
        new TsQueryColumnExtensionMethods(c)
      }
  }
}
