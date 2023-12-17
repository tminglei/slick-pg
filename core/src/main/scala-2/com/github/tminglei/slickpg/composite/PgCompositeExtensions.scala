package com.github.tminglei.slickpg.composite

import slick.jdbc.{JdbcTypesComponent, PostgresProfile}

trait Struct extends Product

trait PgCompositeExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  //TODO not implemented by now
}
