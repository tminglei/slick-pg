package com.github.tminglei.slickpg

trait MyPostgresProfile extends ExPostgresProfile
                          with PgArraySupport
                          with PgDateSupport
                          with PgDate2Support
                          with PgJsonSupport
                          with PgNetSupport
                          with PgLTreeSupport
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgSearchSupport {

  override val pgjson = "jsonb"
  ///
  override val api: MyAPI = new MyAPI {}

  trait MyAPI extends ExtPostgresAPI with ArrayImplicits
                              with SimpleDateTimeImplicits
                              with Date2DateTimeImplicitsDuration
                              with SimpleJsonImplicits
                              with NetImplicits
                              with LTreeImplicits
                              with RangeImplicits
                              with HStoreImplicits
                              with SearchImplicits
                              with SearchAssistants
  ///
  val plainAPI = new MyAPI with ByteaPlainImplicits
                         with SimpleArrayPlainImplicits
                         with Date2DateTimePlainImplicits
                         with SimpleJsonPlainImplicits
                         with SimpleNetPlainImplicits
                         with SimpleLTreePlainImplicits
                         with SimpleRangePlainImplicits
                         with SimpleHStorePlainImplicits
                         with SimpleSearchPlainImplicits {}
}

object MyPostgresProfile extends MyPostgresProfile
