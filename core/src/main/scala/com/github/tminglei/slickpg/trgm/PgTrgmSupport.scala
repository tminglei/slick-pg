package com.github.tminglei.slickpg
package trgm

import slick.jdbc.PostgresProfile

/**
  * Created by minglei on 6/21/17.
  */
trait PgTrgmSupport extends PgTrgmExtensions { driver: PostgresProfile =>
  import driver.api._

  trait PgTrgmImplicits {
    implicit def pgTrgmColumnExtensionMethods(c: Rep[String]): PgTrgmColumnExtensionMethods[String] = new PgTrgmColumnExtensionMethods[String](c)
    implicit def pgTrgmOptionColumnExtensionMethods(c: Rep[Option[String]]): PgTrgmColumnExtensionMethods[Option[String]] = new PgTrgmColumnExtensionMethods[Option[String]](c)
  }
}
