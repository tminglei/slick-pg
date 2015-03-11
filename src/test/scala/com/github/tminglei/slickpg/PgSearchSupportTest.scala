package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import slick.jdbc.GetResult

import scala.concurrent.ExecutionContext.Implicits.global

class PgSearchSupportTest {
  import MyPostgresDriver.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

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
    db.run(DBIO.seq(
      Tests.schema create,
      ///
      Tests forceInsertAll List(testRec1, testRec2),
      // 0. 'get_current_ts_config'
      Tests.filter(_.id === 33L).map(r => currTsConfig()).result.head.map(
        assertEquals("english", _)
      ),
      // 1. '@@'
      Tests.filter(r => toTsVector(r.text, Some("english")) @@ tsQuery("cat & rat")).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec1), _)
      ),
      // 2. '||'
      Tests.filter(r => (toTsVector(r.text) @+ toTsVector(r.comment)) @@ tsQuery("cat & fat".bind)).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec1, testRec2), _)
      ),
      // 3. '&&'/'||'/'!!'
      Tests.filter(r => toTsVector(r.text) @@ (tsQuery("cat".bind) @& tsQuery("rat".bind))).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec1), _)
      ),
      Tests.filter(r => toTsVector(r.text) @@ (tsQuery("cat".bind) @| tsQuery("rat".bind))).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec1, testRec2), _)
      ),
      Tests.filter(r => toTsVector(r.text) @@ (tsQuery("cat".bind) @& tsQuery("rat".bind).!!)).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec2), _)
      ),
      // 4. '@>'
      Tests.filter(r => plainToTsQuery(r.text, Some("english")) @> toTsQuery(r.comment, Some("english"))).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec1), _)
      ),
      // 5. 'length'/'strip'/'setweight'/'numnode'
      Tests.filter(_.id === 33L).map(r => toTsVector(r.text).length).result.head.map(
        assertEquals(4, _)
      ),
      Tests.filter(_.id === 33L).map(r => toTsVector(r.text).strip).result.head.map(
        assertEquals(TsVector("'ate' 'cat' 'fat' 'rat'"), _)
      ),
      Tests.filter(_.id === 33L).map(r => toTsVector(r.text).setWeight('A')).result.head.map(
        assertEquals(TsVector("'ate':3A 'cat':2A 'fat':1A 'rat':4A"), _)
      ),
      Tests.filter(_.id === 33L).map(r => tsQuery("(fat & rat) | cat").numNode).result.head.map(
        assertEquals(5, _)
      ),
      // 6. 'querytree'/'ts_rewrite'
      Tests.filter(_.id === 37L).map(r => toTsQuery(r.text).queryTree).result.head.map(
        assertEquals("'cat'", _)
      ),
      Tests.filter(_.id === 33L).map(r => tsQuery("a & b").rewrite(tsQuery("a"), tsQuery("foo|bar"))).result.head.map(
        assertEquals(TsQuery("'b' & ( 'foo' | 'bar' )"), _)
      ),
      Tests.filter(_.id === 33L).map(r => tsQuery("a & b").rewrite("select 'a'::tsquery, 'foo|bar'::tsquery")).result.head.map(
        assertEquals(TsQuery("'b' & ( 'bar' | 'foo' )"), _)
      ),
      ///
      Tests.schema drop
    ).transactionally)
  }

  @Test
  def testOtherFunctions(): Unit = {
    val query = tsQuery("neutrino|(dark & matter)".bind)

    db.run(DBIO.seq(
      Tests.schema create,
      ///
      Tests.forceInsert(TestBean(11L, "Neutrinos in the Sun", "")),
      Tests.forceInsert(TestBean(12L, "The Sudbury Neutrino Detector", "")),
      Tests.forceInsert(TestBean(13L, "A MACHO View of Galactic Dark Matter", "")),
      Tests.forceInsert(TestBean(14L, "Hot Gas and Dark Matter", "")),
      Tests.forceInsert(TestBean(15L, "The Virgo Cluster: Hot Plasma and Dark Matter", "")),
      Tests.forceInsert(TestBean(16L, "Rafting for Solar Neutrinos", "")),
      Tests.forceInsert(TestBean(17L, "NGC 4650A: Strange Galaxy and Dark Matter", "")),
      Tests.forceInsert(TestBean(18L, "Hot Gas and Dark Matter", "")),
      Tests.forceInsert(TestBean(19L, "Ice Fishing for Cosmic Neutrinos", "")),
      Tests.forceInsert(TestBean(20L, "Weak Lensing Distorts the Universe", "")),
      //
      Tests.filter(r => toTsVector(r.text) @@ query).map(r => (r.id, r.text, tsRank(toTsVector(r.text), query))).sortBy(_._3).result.map(
        _.map { r => println(s"${r._1}  |  ${r._2} | ${r._3}") }
      ),
      Tests.filter(r => toTsVector(r.text) @@ query).map(r => (r.id, r.text, tsRankCD(toTsVector(r.text), query))).sortBy(_._3).result.map(
        _.map { r => println(s"${r._1}  |  ${r._2} | ${r._3}") }
      ),
      Tests.filter(r => toTsVector(r.text) @@ query).map(r => (r.id, r.text, tsHeadline(r.text, query, Some("english")))).result.map(
        _.map { r => println(s"${r._1}  |  ${r._2} | ${r._3}") }
      ),
      ///
      Tests.schema drop
    ).transactionally)
  }

  //--------------------------------------------------------------------------------

  @Test
  def testPlainSearchFunctions(): Unit = {
    import MyPlainPostgresDriver.plainAPI._

    case class SearchBean(id: Long, tVec: TsVector, tQ: TsQuery)

    implicit val getSearchBeanResult = GetResult(r => SearchBean(r.nextLong(), r.nextTsVector(), r.nextTsQuery))

    val b = SearchBean(101L, TsVector("'ate' 'cat' 'fat' 'rat'"), TsQuery("'rat'"))

    db.run(DBIO.seq(
      sqlu"""create table tsTestTable(
            |  id int8 not null primary key,
            |  tsvec tsvector not null,
            |  tsquery tsquery)
          """,
      ///
      sqlu"insert into tsTestTable values(${b.id}, ${b.tVec}, ${b.tQ})",
      sql"select * from tsTestTable where id = ${b.id}".as[SearchBean].head.map(
        assertEquals(b, _)
      ),
      ///
      sqlu"drop table if exists tsTestTable cascade"
    ).transactionally)
  }
}