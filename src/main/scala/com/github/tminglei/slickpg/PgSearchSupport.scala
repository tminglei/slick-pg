package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver

trait PgSearchSupport extends search.PgSearchExtensions { driver: PostgresDriver =>
  import driver.Implicit._

  /// alias
  trait SearchImplicits extends SimpleSearchImplicits

  trait SimpleSearchImplicits {
    implicit def simpleTsVectorColumnExtensionMethods0(c: TsVector) = {
      new TsVectorColumnExtensionMethods(c)
    }
    implicit def simpleTsQueryColumnExtensionMethods0(c: TsQuery) = {
      new TsQueryColumnExtensionMethods(c)
    }
  }
}
