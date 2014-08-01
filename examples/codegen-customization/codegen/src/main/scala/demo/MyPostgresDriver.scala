package demo

import com.github.tminglei.slickpg._

trait MyPostgresDriver extends ExPostgresDriver
                          with PgArraySupport
                          with PgDateSupportJoda
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgSearchSupport
                          with PgPostGISSupport {
  ///
  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
                         with ArrayImplicits
                         with DateTimeImplicits
                         with RangeImplicits
                         with HStoreImplicits
                         with SearchImplicits
                         with PostGISImplicits

  trait SimpleQLPlus extends SimpleQL
                        with ImplicitsPlus
                        with SearchAssistants
}

object MyPostgresDriver extends MyPostgresDriver
