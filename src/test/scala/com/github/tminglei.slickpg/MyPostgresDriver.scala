package com.github.tminglei.slickpg

import slick.driver.PostgresDriver

trait MyPostgresDriver extends PostgresDriver
                          with PgArraySupport
                          with PgDateSupport
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgJsonSupport[text.Document]
                          with PgSearchSupport
                          with PgPostGISSupport {

  override val jsonMethods = org.json4s.native.JsonMethods

  override val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
                        with ArrayImplicits
                        with DatetimeImplicits
                        with RangeImplicits
                        with HStoreImplicits
                        with JsonImplicits
                        with SearchImplicits
                        with PostGISImplicits

  trait SimpleQLPlus extends SimpleQL
                        with ImplicitsPlus
                        with SearchAssistants
                        with PostGISAssistants
}

object MyPostgresDriver extends MyPostgresDriver
