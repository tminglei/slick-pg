package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver

trait PgSearchSupport extends search.PgSearchExtensions { driver: PostgresDriver =>
  import driver.Implicit._

  trait SearchImplicits {
    implicit def TsVectorColumnExtensionMethods0(c: TsVector) = {
      new TsVectorColumnExtensionMethods(c)
    }
    implicit def TsQueryColumnExtensionMethods0(c: TsQuery) = {
      new TsQueryColumnExtensionMethods(c)
    }
  }
}
