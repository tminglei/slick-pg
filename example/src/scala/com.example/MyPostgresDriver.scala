package com.example

import slick.driver.PostgresDriver
import com.github.tminglei.slickpg._

trait MyPostgresDriver extends PostgresDriver
                          with PgArraySupport
                          with PgRangeSupport
                          with PgHStoreSupport
//                          with PgJsonSupport[text.Document]
                          with PgSearchSupport
                          with PostGISSupport
                          with PgDatetimeSupport {

//  override val jsonMethods = org.json4s.native.JsonMethods

  override val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
                         with ArrayImplicits
                         with RangeImplicits
                         with HStoreImplicits
//                         with JsonImplicits
                         with SearchImplicits
                         with PostGISImplicits
                         with DatetimeImplicits

  trait SimpleQLPlus extends SimpleQL
                        with ImplicitsPlus
                        with SearchAssistants
                        with PostGISAssistants
}

object MyPostgresDriver extends MyPostgresDriver
