package com.github.tminglei.slickpg
package trgm

import slick.jdbc.PostgresProfile

/**
  * Created by minglei on 6/21/17.
  */
trait PgTrgmSupport extends PgTrgmExtensions { driver: PostgresProfile =>
  import driver.api._

  trait PgTrgmImplicits {
    implicit def pgStringColumnExtensionMethods(c: Rep[String]) = new PgTrgmColumnExtensionMethods[String](c)
    implicit def pgStringOptionColumnExtensionMethods(c: Rep[Option[String]]) = new PgTrgmColumnExtensionMethods[Option[String]](c)
  }
}
