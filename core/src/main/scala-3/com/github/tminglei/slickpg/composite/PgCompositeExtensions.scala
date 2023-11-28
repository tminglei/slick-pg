package com.github.tminglei.slickpg.composite

import izumi.reflect.macrortti.LightTypeTag
import izumi.reflect.Tag
import slick.jdbc.{JdbcTypesComponent, PostgresProfile}
import scala.annotation.static
import scala.collection.concurrent.TrieMap
import scala.reflect.{classTag, ClassTag}

trait Struct extends AnyRef

trait PgCompositeExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  //TODO not implemented by now
}
