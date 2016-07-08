package com.github.tminglei.slickpg
package composite

import slick.jdbc.{JdbcTypesComponent, PostgresProfile}

trait Struct extends AnyRef

trait PgCompositeExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  //TODO not implemented by now
}
