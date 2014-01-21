package com.github.tminglei.slickpg

import slick.driver.PostgresDriver

trait MyPostgresDriver extends PostgresDriver
                          with PgArraySupport
                          with PgDateSupport
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgJsonSupport
                          with PgSearchSupport
                          with PgPostGISSupport {
  /// for json support
  type DOCType = text.Document
  override val jsonMethods = org.json4s.native.JsonMethods

  ///
  override val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
                        with ArrayImplicits
                        with DateTimeImplicits
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

///
object MyPostgresDriver2 extends PostgresDriver
                            with PgDateSupport2bp {

  override val Implicit = new Implicits with DateTimeImplicits
  override val simple = new Implicits with SimpleQL with DateTimeImplicits
}

///
object MyPostgresDriver3 extends PostgresDriver
                            with PgDateSupportJoda {

  override val Implicit = new Implicits with DateTimeImplicits
  override val simple = new Implicits with SimpleQL with DateTimeImplicits
}

///
object MyPostgresDriver4 extends PostgresDriver
                            with PgPlayJsonSupport
                            with PgArraySupport {

  override val Implicit = new Implicits with JsonImplicits with ArrayImplicits
  override val simple = new Implicits with SimpleQL with JsonImplicits with ArrayImplicits
}