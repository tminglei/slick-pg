package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver

trait PgSearchSupport2 extends search.PgSearchExtensions { driver: PostgresDriver =>

  trait SearchImplicits {
    implicit def TsVectorColumnExtensionMethods0(c: TsVector[String]) = new TsVectorColumnExtensionMethods(c)
    implicit def TsVectorOptionColumnExtensionMethods(c: TsVector[Option[String]]) = new TsVectorColumnExtensionMethods(c)
    implicit def TsQueryColumnExtensionMethods0(c: TsQuery[String]) = new TsQueryColumnExtensionMethods(c)
    implicit def TsQueryOptionColumnExtensionMethods(c: TsQuery[Option[String]]) = new TsQueryColumnExtensionMethods(c)
  }
}
