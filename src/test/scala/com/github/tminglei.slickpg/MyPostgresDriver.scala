package com.github.tminglei.slickpg

import slick.driver.PostgresDriver

trait MyPostgresDriver extends PostgresDriver
                          with PgArraySupport
                          with PgDatetimeSupport
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgSearchSupport
                          with PostGISSupport {

  override val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
                        with ArrayImplicits
                        with DateImplicits
                        with RangeImplicits
                        with HStoreImplicits
                        with SearchImplicits
                        with PostGISImplicits

  trait SimpleQLPlus extends SimpleQL
                        with ImplicitsPlus
                        with SearchAssistants
                        with PostGISAssistants
}

object MyPostgresDriver extends MyPostgresDriver
