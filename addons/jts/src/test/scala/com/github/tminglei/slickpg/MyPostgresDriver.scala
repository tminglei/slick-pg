package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver

object MyPostgresDriver extends PostgresDriver
                           with PgPostGISSupport {

  override lazy val Implicit = new Implicits with PostGISImplicits
  override val simple = new Implicits with SimpleQL with PostGISImplicits
                              with PostGISAssistants
}
