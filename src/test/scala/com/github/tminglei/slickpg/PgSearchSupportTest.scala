package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import scala.util.Try

class PgSearchSupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=postgres", driver = "org.postgresql.Driver")

  case class TestBean(id: Long, text: String, comment: String)

  class TestTable(tag: Tag) extends Table[TestBean](tag, "tsTestTable") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def text = column[String]("text")
    def comment = column[String]("comment")

    def * = (id, text, comment) <> (TestBean.tupled, TestBean.unapply)
  }
  val Tests = TableQuery[TestTable]

  //-----------------------------------------------------------------------------

  val testRec1 = TestBean(33L, "fat cat ate rat", "")
  val testRec2 = TestBean(37L, "cat", "fat")

  @Test
  def testSearchFunctions(): Unit = {
    db withSession { implicit session: Session =>
      Tests.forceInsertAll(testRec1, testRec2)

      val q0 = Tests.filter(_.id === 33L).map(r => currTsConfig())
      assertEquals("english", q0.first)

      val q1 = Tests.filter(r => toTsVector(r.text, Some("english")) @@ tsQuery("cat & rat")).sortBy(_.id).map(t => t)
      println(s"[search] '@@' sql = ${q1.selectStatement}")
      assertEquals(List(testRec1), q1.list)

      val q2 = Tests.filter(r => (toTsVector(r.text) @+ toTsVector(r.comment)) @@ tsQuery("cat & fat".bind)).sortBy(_.id).map(t => t)
      println(s"[search] '@+' sql = ${q2.selectStatement}")
      assertEquals(List(testRec1, testRec2), q2.list)

      val q3 = Tests.filter(r => toTsVector(r.text) @@ (tsQuery("cat".bind) @& tsQuery("rat".bind))).sortBy(_.id).map(t => t)
      println(s"[search] '@&' sql = ${q3.selectStatement}")
      assertEquals(List(testRec1), q3.list)

      val q4 = Tests.filter(r => toTsVector(r.text) @@ (tsQuery("cat".bind) @| tsQuery("rat".bind))).sortBy(_.id).map(t => t)
      println(s"[search] '@|' sql = ${q4.selectStatement}")
      assertEquals(List(testRec1, testRec2), q4.list)

      val q5 = Tests.filter(r => toTsVector(r.text) @@ (tsQuery("cat".bind) @& tsQuery("rat".bind).!!)).sortBy(_.id).map(t => t)
      println(s"[search] '!!' sql = ${q5.selectStatement}")
      assertEquals(List(testRec2), q5.list)

      val q6 = Tests.filter(r => plainToTsQuery(r.text, Some("english")) @> toTsQuery(r.comment, Some("english"))).sortBy(_.id).map(t => t)
      println(s"[search] '@>' sql = ${q6.selectStatement}")
      assertEquals(List(testRec1), q6.list)

      val q7 = Tests.filter(_.id === 33L).map(r => toTsVector(r.text).length)
      println(s"[search] 'length' sql = ${q7.selectStatement}")
      assertEquals(4, q7.first)

      val q8 = Tests.filter(_.id === 33L).map(r => toTsVector(r.text).strip)
      println(s"[search] 'strip' sql = ${q8.selectStatement}")
      assertEquals("'ate' 'cat' 'fat' 'rat'", q8.first)

      val q9 = Tests.filter(_.id === 33L).map(r => toTsVector(r.text).setWeight('A'))
      println(s"[search] 'setWeight' sql = ${q9.selectStatement}")
      assertEquals("'ate':3A 'cat':2A 'fat':1A 'rat':4A", q9.first)

      val q10 = Tests.filter(_.id === 33L).map(r => tsQuery("(fat & rat) | cat").numNode)
      println(s"[search] 'numNode' sql = ${q10.selectStatement}")
      assertEquals(5, q10.first)

      val q11 = Tests.filter(_.id === 37L).map(r => toTsQuery(r.text).queryTree)
      println(s"[search] 'queryTree' sql = ${q11.selectStatement}")
      assertEquals("'cat'", q11.first)

      val q12 = Tests.filter(_.id === 33L).map(r => tsQuery("a & b").rewrite(tsQuery("a"), tsQuery("foo|bar")))
      println(s"[search] 'rewrite' sql = ${q12.selectStatement}")
      assertEquals("'b' & ( 'foo' | 'bar' )", q12.first)

      val q13 = Tests.filter(_.id === 33L).map(r => tsQuery("a & b").rewrite("select 'a'::tsquery, 'foo|bar'::tsquery"))
      println(s"[search] 'rewrite2' sql = ${q13.selectStatement}")
      assertEquals("'b' & ( 'bar' | 'foo' )", q13.first)
    }
  }

  @Test
  def testOtherFunctions(): Unit = {
    db withSession { implicit session: Session =>
      Tests.forceInsert(TestBean(11L, "Neutrinos in the Sun", ""))
      Tests.forceInsert(TestBean(12L, "The Sudbury Neutrino Detector", ""))
      Tests.forceInsert(TestBean(13L, "A MACHO View of Galactic Dark Matter", ""))
      Tests.forceInsert(TestBean(14L, "Hot Gas and Dark Matter", ""))
      Tests.forceInsert(TestBean(15L, "The Virgo Cluster: Hot Plasma and Dark Matter", ""))
      Tests.forceInsert(TestBean(16L, "Rafting for Solar Neutrinos", ""))
      Tests.forceInsert(TestBean(17L, "NGC 4650A: Strange Galaxy and Dark Matter", ""))
      Tests.forceInsert(TestBean(18L, "Hot Gas and Dark Matter", ""))
      Tests.forceInsert(TestBean(19L, "Ice Fishing for Cosmic Neutrinos", ""))
      Tests.forceInsert(TestBean(20L, "Weak Lensing Distorts the Universe", ""))

      val query = tsQuery("neutrino|(dark & matter)".bind)

      val q1 = Tests.filter(r => toTsVector(r.text) @@ query).map(r => (r.id, r.text, tsRank(toTsVector(r.text), query))).sortBy(_._3)
      println(s"[search] 'ts_rank' sql = ${q1.selectStatement}")
      q1.list.map(r => println(s"${r._1}  |  ${r._2} | ${r._3}"))

      val q2 = Tests.filter(r => toTsVector(r.text) @@ query).map(r => (r.id, r.text, tsRankCD(toTsVector(r.text), query))).sortBy(_._3)
      println(s"[search] 'ts_rank_cd' sql = ${q2.selectStatement}")
      q2.list.map(r => println(s"${r._1}  |  ${r._2} | ${r._3}"))

      val q3 = Tests.filter(r => toTsVector(r.text) @@ query).map(r => (r.id, r.text, tsHeadline(r.text, query, Some("english"))))
      println(s"[search] 'ts_headline' sql = ${q3.selectStatement}")
      q3.list.map(r => println(s"${r._1}  |  ${r._2} | ${r._3}"))
    }
  }

  //--------------------------------------------------------------------------------

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      Try { Tests.ddl drop }
      Try { Tests.ddl create }
    }
  }
}
