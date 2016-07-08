package com.github.tminglei.slickpg

trait MyPostgresProfile extends ExPostgresProfile
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
  override val api = new API with ArrayImplicits
                             with DateTimeImplicits
                             with SimpleJsonImplicits
                             with NetImplicits
                             with LTreeImplicits
                             with RangeImplicits
                             with HStoreImplicits
                             with SearchImplicits
                             with SearchAssistants {}
  ///
  val plainAPI = new API with SimpleArrayPlainImplicits
                         with SimpleJsonPlainImplicits
                         with SimpleNetPlainImplicits
                         with SimpleLTreePlainImplicits
                         with SimpleRangePlainImplicits
                         with SimpleHStorePlainImplicits
                         with SimpleSearchPlainImplicits {}
}

object MyPostgresProfile extends MyPostgresProfile
