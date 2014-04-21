package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver

object MyPostgresDriver extends PostgresDriver
                           with PgDate2Support {

  override val Implicit = new Implicits with DateTimeImplicits
  override val simple = new Implicits with SimpleQL with DateTimeImplicits
}
