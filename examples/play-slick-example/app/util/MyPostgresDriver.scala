package util

import com.github.tminglei.slickpg._

trait MyPostgresDriver extends ExPostgresDriver
                          with PgArraySupport
                          with PgDate2Support
                          with PgPlayJsonSupport
                          with PgNetSupport
                          with PgLTreeSupport
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgSearchSupport {

  override val pgjson = "jsonb"
  ///
  override val api = new ExtPostgresAPI with ArrayImplicits
                             with Date2DateTimeImplicitsDuration
                             with PlayJsonImplicits
                             with NetImplicits
                             with LTreeImplicits
                             with RangeImplicits
                             with HStoreImplicits
                             with SearchImplicits
                             with SearchAssistants {}
}

object MyPostgresDriver extends MyPostgresDriver
