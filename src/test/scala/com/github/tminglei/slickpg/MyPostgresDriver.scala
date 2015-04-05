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
  override val api = new API with ArrayImplicits
                             with DateTimeImplicits
                             with SimpleJsonImplicits
                             with NetImplicits
                             with LTreeImplicits
                             with RangeImplicits
                             with HStoreImplicits
                             with SearchImplicits
                             with SearchAssistants {}
}

object MyPostgresDriver extends MyPostgresDriver

/// for plain sql tests
import slick.driver.PostgresDriver
import scala.reflect.runtime.{universe => u}
import slick.jdbc.PositionedResult

import utils.SimpleArrayUtils._

object MyPlainPostgresDriver extends PostgresDriver
                              with PgArraySupport
                              with PgJsonSupport
                              with PgNetSupport
                              with PgLTreeSupport
                              with PgRangeSupport
                              with PgHStoreSupport
                              with PgSearchSupport {
  override val pgjson = "jsonb"
  ///
  val plainAPI = new API with SimpleArrayPlainImplicits
                         with SimpleJsonPlainImplicits
                         with SimpleNetPlainImplicits
                         with SimpleLTreePlainImplicits
                         with SimpleRangePlainImplicits
                         with SimpleHStorePlainImplicits
                         with SimpleSearchPlainImplicits {
    override protected def extNextArray(tpe: u.Type, r: PositionedResult): (Boolean, Option[Seq[_]]) =
      tpe match {
        case tpe if tpe.typeConstructor =:= u.typeOf[LTree].typeConstructor =>
          (true, r.nextStringOption().flatMap(fromString(LTree.apply)))
        case _ => super.extNextArray(tpe, r)
      }
  }
}
