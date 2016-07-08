package com.github.tminglei.slickpg

import org.scalatest.FunSuite

import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration._

class PgLTreeSupportSuite extends FunSuite {
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class LTreeBean(id: Long, path: LTree, treeArr: List[LTree])

  class LTreeTestTable(tag: Tag) extends Table[LTreeBean](tag, "ltree_test") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def path = column[LTree]("path")
    def treeArr = column[List[LTree]]("tree_arr")

    def * = (id, path, treeArr) <> (LTreeBean.tupled, LTreeBean.unapply)
  }
  val LTreeTests = TableQuery[LTreeTestTable]

  //------------------------------------------------------------------------------

  val rec1 = LTreeBean(101L, LTree("Top"), List(LTree("Top.Science"), LTree("Top.Collections")))
  val rec2 = LTreeBean(102L, LTree("Top.Science"), List(LTree("Top.Science.Astronomy")))
  val rec3 = LTreeBean(103L, LTree("Top.Science.Astronomy"), List(LTree("Top.Science.Astronomy.Astrophysics"), LTree("Top.Science.Astronomy.Cosmology")))
  val rec4 = LTreeBean(104L, LTree("Top.Science.Astronomy.Astrophysics"), Nil)
  val rec5 = LTreeBean(105L, LTree("Top.Science.Astronomy.Cosmology"), Nil)
  val rec6 = LTreeBean(106L, LTree("Top.Hobbies"), List(LTree("Top.Hobbies.Amateurs_Astronomy")))
  val rec7 = LTreeBean(107L, LTree("Top.Hobbies.Amateurs_Astronomy"), Nil)
  val rec8 = LTreeBean(108L, LTree("Top.Collections"), List(LTree("Top.Collections.Pictures")))
  val rec9 = LTreeBean(109L, LTree("Top.Collections.Pictures"), List(LTree("Top.Collections.Pictures.Astronomy")))
  val rec10 = LTreeBean(110L, LTree("Top.Collections.Pictures.Astronomy"),
    List(LTree("Top.Collections.Pictures.Astronomy.Stars"), LTree("Top.Collections.Pictures.Astronomy.Galaxies"), LTree("Top.Collections.Pictures.Astronomy.Astronauts")))
  val rec11 = LTreeBean(111L, LTree("Top.Collections.Pictures.Astronomy.Stars"), Nil)
  val rec12 = LTreeBean(112L, LTree("Top.Collections.Pictures.Astronomy.Galaxies"), Nil)
  val rec13 = LTreeBean(113L, LTree("Top.Collections.Pictures.Astronomy.Astronauts"), Nil)

  test("Ltree Lifted support - Ltree") {
    Await.result(db.run(
      DBIO.seq(
        LTreeTests.schema create,
        ///
        LTreeTests forceInsertAll List(rec1, rec2, rec3, rec4, rec5, rec6, rec7, rec8, rec9, rec10, rec11, rec12, rec13)
      ).andThen(
        DBIO.seq(
          LTreeTests.filter(_.id === 101L).result.head.map(
            r => assert(rec1 === r)
          ),
          // @>
          LTreeTests.filter(_.path @> LTree("Top.Science")).result.head.map(
            r => assert(rec1 === r)
          ),
          // ~
          LTreeTests.filter(_.path ~ "Top.Science").result.head.map(
            r => assert(rec2 === r)
          ),
          // ?
          LTreeTests.filter(_.path ~| List("*.Astronomy.Astro*")).to[List].result.map(
            r => assert(List(rec4, rec13) === r)
          ),
          // <@
          LTreeTests.filter(LTree("Top.Science").bind <@: _.path).result.head.map(
            r => assert(rec1 === r)
          ),
          // ||
          LTreeTests.filter(_.id === 101L).map(_.path || LTree("Test")).result.head.map(
            r => assert(LTree("Top.Test") === r)
          ),
          LTreeTests.filter(_.id === 101L).map(_.path || "Test").result.head.map(
            r => assert(LTree("Top.Test") === r)
          ),
          LTreeTests.filter(_.id === 101L).map("Test" ||: _.path).result.head.map(
            r => assert(LTree("Test.Top") === r)
          ),
          // subltree
          LTreeTests.filter(_.id === 105L).map(_.path.subltree(1, 3)).result.head.map(
            r => assert(LTree("Science.Astronomy") === r)
          ),
          // subpath
          LTreeTests.filter(_.id === 105L).map(_.path.subpath(1)).result.head.map(
            r => assert(LTree("Science.Astronomy.Cosmology") === r)
          ),
          LTreeTests.filter(_.id === 105L).map(_.path.subpath(1, Some(2))).result.head.map(
            r => assert(LTree("Science.Astronomy") === r)
          ),
          // nlevel
          LTreeTests.filter(_.id === 105L).map(_.path.nlevel()).result.head.map(
            r => assert(4 === r)
          ),
          // index
          LTreeTests.filter(_.id === 105L).map(_.path.index(LTree("Astronomy.Cosmology"))).result.head.map(
            r => assert(2 === r)
          ),
          LTreeTests.filter(_.id === 105L).map(_.path.index(LTree("Science"), Some(-4))).result.head.map(
            r => assert(1 === r)
          )
        )
      ).andFinally(
        LTreeTests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  test("Ltree Lifted support - Ltree[]") {
    Await.result(db.run(
      DBIO.seq(
        LTreeTests.schema create,
        ///
        LTreeTests forceInsertAll List(rec1, rec2, rec3, rec4, rec5, rec6, rec7, rec8, rec9, rec10, rec11, rec12, rec13)
      ).andThen(
        DBIO.seq(
          // @>
          LTreeTests.filter(_.treeArr @> LTree("Top.Science")).result.head.map(
            r => assert(rec1 === r)
          ),
          // <@
          LTreeTests.filter(LTree("Top.Science").bind <@: _.treeArr).result.head.map(
            r => assert(rec1 === r)
          ),
          // ~
          LTreeTests.filter(_.treeArr ~ "*.Astronomy.Astro*").to[List].result.map(
            r => assert(List(rec3, rec10) === r)
          ),
          // ?
          LTreeTests.filter(_.treeArr ~| List("*.Astronomy.Astro*")).to[List].result.map(
            r => assert(List(rec3, rec10) === r)
          ),
          // @
          LTreeTests.filter(_.treeArr @@ "Astro* & !pictures@").to[List].result.map(
            r => assert(List(rec2, rec3) === r)
          ),
          // lca
          LTreeTests.filter(_.id === 101L).map(_.treeArr.lca).result.head.map(
            r => assert(LTree("Top") === r)
          ),
          // ?@>
          LTreeTests.filter(_.id === 101L).map(_.treeArr ?@> LTree("Top.Science.Astronomy.Astrophysics")).result.head.map(
            r => assert(LTree("Top.Science") === r)
          ),
          // ?<@
          LTreeTests.filter(_.id === 103L).map(_.treeArr ?<@ LTree("Top.Science.Astronomy")).result.head.map(
            r => assert(LTree("Top.Science.Astronomy.Astrophysics") === r)
          ),
          // ?~
          LTreeTests.filter(_.id === 103L).map(_.treeArr ?~ "*.Astronomy.Astro*").result.head.map(
            r => assert(LTree("Top.Science.Astronomy.Astrophysics") === r)
          ),
          // ?@
          LTreeTests.filter(_.id === 103L).map(_.treeArr ?@ "Astro* & !pictures@").result.head.map(
            r => assert(LTree("Top.Science.Astronomy.Astrophysics") === r)
          )
        )
      ).andFinally(
        LTreeTests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  //------------------------------------------------------------------------------

  test("Ltree Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getLTreeBeanResult = GetResult(r => LTreeBean(r.nextLong(), r.nextLTree(), r.nextArray[LTree]().toList))

    val b = LTreeBean(100L, LTree("Top"), List(LTree("Top.Science"), LTree("Top.Collections")))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table ltree_test(
              id int8 not null primary key,
              path ltree not null,
              tree_arr ltree[] not null)
          """,
        ///
        sqlu""" insert into ltree_test values(${b.id}, ${b.path}, ${b.treeArr}) """,
        sql""" select * from ltree_test where id = ${b.id} """.as[LTreeBean].head.map(
          r => assert(b === r)
        ),
        ///
        sqlu"drop table if exists ltree_test cascade"
      ).transactionally
    ), Duration.Inf)
  }
}