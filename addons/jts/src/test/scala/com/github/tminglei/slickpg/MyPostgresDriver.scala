package com.github.tminglei.slickpg

import slick.driver.PostgresDriver

object MyPostgresDriver extends PostgresDriver
                           with PgPostGISSupport {

  override val Implicit = new Implicits with PostGISImplicits
  override val simple = new Implicits with SimpleQL with PostGISImplicits
                              with PostGISAssistants
}
