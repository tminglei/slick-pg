package com.github.tminglei.slickpg
package composite

import slick.driver.{PostgresDriver, JdbcTypesComponent}

trait Struct extends AnyRef

trait PgCompositeExtensions extends JdbcTypesComponent { driver: PostgresDriver =>
  //TODO not implemented by now
}
