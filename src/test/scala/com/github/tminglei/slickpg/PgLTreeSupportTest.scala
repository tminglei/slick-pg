package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._

import scala.slick.jdbc.{StaticQuery => Q, GetResult}
import scala.util.Try

class PgLTreeSupportTest {
  import com.github.tminglei.slickpg.MyPostgresDriver.simple._

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
    db withSession { implicit session: Session =>
      Try { LTreeTests.ddl drop }
      Try { LTreeTests.ddl create }

      LTreeTests forceInsertAll (rec1, rec2, rec3, rec4, rec5, rec6, rec7, rec8, rec9, rec10, rec11, rec12, rec13)

      val q0 = LTreeTests.filter(_.id === 101L).map(identity)
      assertEquals(rec1, q0.first)

      val q1 = LTreeTests.filter(_.path @> LTree("Top.Science")).map(identity)
      println(s"[ltree] '@>' sql = ${q1.selectStatement}")
      assertEquals(rec1, q1.first)

      val q2 = LTreeTests.filter(LTree("Top.Science").bind <@: _.path).map(identity)
      println(s"[ltree] '<@' sql = ${q2.selectStatement}")
      assertEquals(rec1, q2.first)

      val q3 = LTreeTests.filter(_.path ~ "*.Astronomy.Astro*")
      println(s"[ltree] '~' sql = ${q3.selectStatement}")
      assertEquals(List(rec4, rec13), q3.list)

      val q31 = LTreeTests.filter(_.path ~| List("*.Astronomy.Astro*"))
      println(s"[ltree] '~|' sql = ${q31.selectStatement}")
      assertEquals(List(rec4, rec13), q31.list)

      val q4 = LTreeTests.filter(_.path @@ "Astro* & !pictures@")
      println(s"[ltree] '@' sql = ${q4.selectStatement}")
      assertEquals(List(rec3, rec4, rec5), q4.list)

      val q5 = LTreeTests.filter(_.id === 101L).map(_.path || LTree("Test"))
      println(s"[ltree] '||' sql = ${q5.selectStatement}")
      assertEquals(LTree("Top.Test"), q5.first)

      val q51 = LTreeTests.filter(_.id === 101L).map(_.path || "Test")
      println(s"[ltree] '||' sql1 = ${q51.selectStatement}")
      assertEquals(LTree("Top.Test"), q51.first)

      val q52 = LTreeTests.filter(_.id === 101L).map("Test" ||: _.path)
      println(s"[ltree] '||' sql2 = ${q52.selectStatement}")
      assertEquals(LTree("Test.Top"), q52.first)

      val q6 = LTreeTests.filter(_.id === 105L).map(_.path.subltree(1, 3))
      println(s"[ltree] 'subltree' sql = ${q6.selectStatement}")
      assertEquals(LTree("Science.Astronomy"), q6.first)

      val q7 = LTreeTests.filter(_.id === 105L).map(_.path.subpath(1))
      println(s"[ltree] 'subpath' sql = ${q7.selectStatement}")
      assertEquals(LTree("Science.Astronomy.Cosmology"), q7.first)

      val q71 = LTreeTests.filter(_.id === 105L).map(_.path.subpath(1, Some(2)))
      println(s"[ltree] 'subpath' sql1 = ${q71.selectStatement}")
      assertEquals(LTree("Science.Astronomy"), q71.first)

      val q8 = LTreeTests.filter(_.id === 105L).map(_.path.nlevel())
      println(s"[ltree] 'nlevel' sql = ${q8.selectStatement}")
      assertEquals(4, q8.first)

      val q9 = LTreeTests.filter(_.id === 105L).map(_.path.index(LTree("Astronomy.Cosmology")))
      println(s"[ltree] 'index' sql = ${q9.selectStatement}")
      assertEquals(2, q9.first)

      val q91 = LTreeTests.filter(_.id === 105L).map(_.path.index(LTree("Science"), Some(-4)))
      println(s"[ltree] 'index' sql1 = ${q91.selectStatement}")
      assertEquals(1, q91.first)
    }
  }

  @Test
  def testLTreeListMethods(): Unit = {
    db withSession { implicit session: Session =>
      Try { LTreeTests.ddl drop }
      Try { LTreeTests.ddl create }

      LTreeTests forceInsertAll (rec1, rec2, rec3, rec4, rec5, rec6, rec7, rec8, rec9, rec10, rec11, rec12, rec13)

      val q1 = LTreeTests.filter(_.treeArr @> LTree("Top.Science")).map(identity)
      println(s"[ltree[] '@>' sql = ${q1.selectStatement}")
      assertEquals(rec1, q1.first)

      val q2 = LTreeTests.filter(LTree("Top.Science").bind <@: _.treeArr).map(identity)
      println(s"[ltree[] '<@' sql = ${q2.selectStatement}")
      assertEquals(rec1, q2.first)

      val q3 = LTreeTests.filter(_.treeArr ~ "*.Astronomy.Astro*")
      println(s"[ltree[] '~' sql = ${q3.selectStatement}")
      assertEquals(List(rec3, rec10), q3.list)

      val q31 = LTreeTests.filter(_.treeArr ~| List("*.Astronomy.Astro*"))
      println(s"[ltree[] '~|' sql = ${q31.selectStatement}")
      assertEquals(List(rec3, rec10), q31.list)

      val q4 = LTreeTests.filter(_.treeArr @@ "Astro* & !pictures@")
      println(s"[ltree[] '@' sql = ${q4.selectStatement}")
      assertEquals(List(rec2, rec3), q4.list)

      val q5 = LTreeTests.filter(_.id === 101L).map(_.treeArr.lca)
      println(s"[ltree[] 'lca' sql = ${q5.selectStatement}")
      assertEquals(LTree("Top"), q5.first)

      val q6 = LTreeTests.filter(_.id === 101L).map(_.treeArr ?@> LTree("Top.Science.Astronomy.Astrophysics"))
      println(s"[ltree[] '?@>' sql = ${q6.selectStatement}")
      assertEquals(LTree("Top.Science"), q6.first)

      val q7 = LTreeTests.filter(_.id === 103L).map(_.treeArr ?<@ LTree("Top.Science.Astronomy"))
      println(s"[ltree[] '?<@' sql = ${q7.selectStatement}")
      assertEquals(LTree("Top.Science.Astronomy.Astrophysics"), q7.first)

      val q8 = LTreeTests.filter(_.id === 103L).map(_.treeArr ?~ "*.Astronomy.Astro*")
      println(s"[ltree[] '?~' sql = ${q8.selectStatement}")
      assertEquals(LTree("Top.Science.Astronomy.Astrophysics"), q8.first)

      val q9 = LTreeTests.filter(_.id === 103L).map(_.treeArr ?@ "Astro* & !pictures@")
      println(s"[ltree[] '?@' sql = ${q9.selectStatement}")
      assertEquals(LTree("Top.Science.Astronomy.Astrophysics"), q9.first)
    }
  }

  //------------------------------------------------------------------------------

  @Test
  def testPlainLTreeFunctions(): Unit = {
    import MyPlainPostgresDriver.plainImplicits._

    implicit val getLTreeBeanResult = GetResult(r => LTreeBean(r.nextLong(), r.nextLTree(), r.nextLTreeArray().toList))

    db withSession { implicit session: Session =>
      Try { Q.updateNA("drop table if exists ltree_test cascade").execute }
      Try {
        Q.updateNA("create table ltree_test(" +
          "id int8 not null primary key, " +
          "path ltree not null, " +
          "tree_arr ltree[] not null)"
        ).execute
      }

      val treeBean = LTreeBean(101L, LTree("Top"), List(LTree("Top.Science"), LTree("Top.Collections")))

      (Q.u + "insert into ltree_test values(" +? treeBean.id + ", " +? treeBean.path + ", " +? treeBean.treeArr + ")").execute

      val found = (Q[LTreeBean] + "select * from ltree_test where id = " +? treeBean.id).first

      assertEquals(treeBean, found)
    }
  }
}
