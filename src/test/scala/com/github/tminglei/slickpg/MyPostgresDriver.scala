package com.github.tminglei.slickpg

trait MyPostgresDriver extends ExPostgresDriver
                          with PgArraySupport
                          with PgDateSupport
                          with PgJsonSupport
                          with PgNetSupport
                          with PgLTreeSupport
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgSearchSupport {
  override val pgjson = "jsonb"
  ///
  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
                        with ArrayImplicits
                        with DateTimeImplicits
                        with SimpleJsonImplicits
                        with NetImplicits
                        with LTreeImplicits
                        with RangeImplicits
                        with HStoreImplicits
                        with SearchImplicits

  trait SimpleQLPlus extends SimpleQL
                        with ImplicitsPlus
                        with SearchAssistants
}

object MyPostgresDriver extends MyPostgresDriver

/// for plain sql tests
import scala.slick.driver.PostgresDriver

object MyPlainPostgresDriver extends PostgresDriver
                              with PgArraySupport
                              with PgJsonSupport
                              with PgNetSupport
                              with PgLTreeSupport
                              with PgRangeSupport
                              with PgHStoreSupport
                              with PgSearchSupport {
  ///
  val plainImplicits = new Implicits with SimpleArrayPlainImplicits
                                     with SimpleJsonPlainImplicits
                                     with SimpleNetPlainImplicits
                                     with SimpleLTreePlainImplicits
                                     with SimpleRangePlainImplicits
                                     with SimpleHStorePlainImplicits
                                     with SimpleSearchPlainImplicits {}
}
