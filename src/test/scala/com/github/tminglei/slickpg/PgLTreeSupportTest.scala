package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._

import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global

class PgLTreeSupportTest {
  import MyPostgresDriver.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

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

  @Test
  def testLTreeMethods(): Unit = {
    db.run(DBIO.seq(
      LTreeTests.schema create,
      ///
      LTreeTests forceInsertAll List(rec1, rec2, rec3, rec4, rec5, rec6, rec7, rec8, rec9, rec10, rec11, rec12, rec13),
      // 0. simple test
      LTreeTests.filter(_.id === 101L).result.head.map(
        assertEquals(rec1, _)
      ),
      // 1. '@>'/'<@'
      LTreeTests.filter(_.path @> LTree("Top.Science")).result.head.map(
        assertEquals(rec1, _)
      ),
      LTreeTests.filter(LTree("Top.Science").bind <@: _.path).result.head.map(
        assertEquals(rec1, _)
      ),
      // 2. '~'/'?'/'@'/'||'
      LTreeTests.filter(_.path ~ "*.Astronomy.Astro*").to[List].result.map(
        assertEquals(List(rec4, rec13), _)
      ),
      LTreeTests.filter(_.path ~| List("*.Astronomy.Astro*")).to[List].result.map(
        assertEquals(List(rec4, rec13), _)
      ),
      LTreeTests.filter(_.path @@ "Astro* & !pictures@").to[List].result.map(
        assertEquals(List(rec3, rec4, rec5), _)
      ),
      LTreeTests.filter(_.id === 101L).map(_.path || LTree("Test")).result.head.map(
        assertEquals(LTree("Top.Test"), _)
      ),
      LTreeTests.filter(_.id === 101L).map(_.path || "Test").result.head.map(
        assertEquals(LTree("Top.Test"), _)
      ),
      LTreeTests.filter(_.id === 101L).map("Test" ||: _.path).result.head.map(
        assertEquals(LTree("Test.Top"), _)
      ),
      // 3. 'subltree'/'subpath'
      LTreeTests.filter(_.id === 105L).map(_.path.subltree(1, 3)).result.head.map(
        assertEquals(LTree("Science.Astronomy"), _)
      ),
      LTreeTests.filter(_.id === 105L).map(_.path.subpath(1)).result.head.map(
        assertEquals(LTree("Science.Astronomy.Cosmology"), _)
      ),
      LTreeTests.filter(_.id === 105L).map(_.path.subpath(1, Some(2))).result.head.map(
        assertEquals(LTree("Science.Astronomy"), _)
      ),
      // 4. 'nlevel'/'index'
      LTreeTests.filter(_.id === 105L).map(_.path.nlevel()).result.head.map(
        assertEquals(4, _)
      ),
      LTreeTests.filter(_.id === 105L).map(_.path.index(LTree("Astronomy.Cosmology"))).result.head.map(
        assertEquals(2, _)
      ),
      LTreeTests.filter(_.id === 105L).map(_.path.index(LTree("Science"), Some(-4))).result.head.map(
        assertEquals(1, _)
      ),
      ///
      LTreeTests.schema drop
    ).transactionally)
  }

  @Test
  def testLTreeListMethods(): Unit = {
    db.run(DBIO.seq(
      LTreeTests.schema create,
      ///
      LTreeTests forceInsertAll List(rec1, rec2, rec3, rec4, rec5, rec6, rec7, rec8, rec9, rec10, rec11, rec12, rec13),
      // 1. '@>'/'<@'
      LTreeTests.filter(_.treeArr @> LTree("Top.Science")).result.head.map(
        assertEquals(rec1, _)
      ),
      LTreeTests.filter(LTree("Top.Science").bind <@: _.treeArr).result.head.map(
        assertEquals(rec1, _)
      ),
      // 2. '~'/'?'/'@'
      LTreeTests.filter(_.treeArr ~ "*.Astronomy.Astro*").to[List].result.map(
        assertEquals(List(rec3, rec10), _)
      ),
      LTreeTests.filter(_.treeArr ~| List("*.Astronomy.Astro*")).to[List].result.map(
        assertEquals(List(rec3, rec10), _)
      ),
      LTreeTests.filter(_.treeArr @@ "Astro* & !pictures@").to[List].result.map(
        assertEquals(List(rec2, rec3), _)
      ),
      // 3. 'lca'
      LTreeTests.filter(_.id === 101L).map(_.treeArr.lca).result.head.map(
        assertEquals(LTree("Top"), _)
      ),
      // 4. '?@>'/'?<@'/'?~'/'?@'
      LTreeTests.filter(_.id === 101L).map(_.treeArr ?@> LTree("Top.Science.Astronomy.Astrophysics")).result.head.map(
        assertEquals(LTree("Top.Science"), _)
      ),
      LTreeTests.filter(_.id === 103L).map(_.treeArr ?<@ LTree("Top.Science.Astronomy")).result.head.map(
        assertEquals(LTree("Top.Science.Astronomy.Astrophysics"), _)
      ),
      LTreeTests.filter(_.id === 103L).map(_.treeArr ?~ "*.Astronomy.Astro*").result.head.map(
        assertEquals(LTree("Top.Science.Astronomy.Astrophysics"), _)
      ),
      LTreeTests.filter(_.id === 103L).map(_.treeArr ?@ "Astro* & !pictures@").result.head.map(
        assertEquals(LTree("Top.Science.Astronomy.Astrophysics"), _)
      ),
      ///
      LTreeTests.schema drop
    ).transactionally)
  }

  //------------------------------------------------------------------------------

  @Test
  def testPlainLTreeFunctions(): Unit = {
    import MyPlainPostgresDriver.plainAPI._

    implicit val getLTreeBeanResult = GetResult(r => LTreeBean(r.nextLong(), r.nextLTree(), r.nextArray[LTree]().toList))

    val b = LTreeBean(101L, LTree("Top"), List(LTree("Top.Science"), LTree("Top.Collections")))

    db.run(DBIO.seq(
      sqlu"""create table ltree_test(
            |  id int8 not null primary key,
            |  path ltree not null,
            |  tree_arr ltree[] not null)
          """,
      ///
      sqlu"insert into ltree_test values(${b.id}, ${b.path}, ${b.treeArr})",
      sql"select * from ltree_test where id = ${b.id}".as[LTreeBean].head.map(
        assertEquals(b, _)
      ),
      ///
      sqlu"drop table if exists ltree_test cascade"
    ).transactionally)
  }
}