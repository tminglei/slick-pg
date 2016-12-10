package com.github.tminglei.slickpg

import org.scalatest.FunSuite
import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration._

class PgSearchSupportSuite extends FunSuite {
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class TestBean(id: Long, text: String, search: TsVector, comment: String)

  class TestTable(tag: Tag) extends Table[TestBean](tag, "tsTestTable") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def text = column[String]("text")
    def search = column[TsVector]("search")
    def comment = column[String]("comment")

    def * = (id, text, search, comment) <> (TestBean.tupled, TestBean.unapply)
  }
  val Tests = TableQuery[TestTable]

  //-----------------------------------------------------------------------------

  val testRec1 = TestBean(33L, "fat cat ate rat", TsVector("'ate' 'cat' 'fat' 'rat'"), "")
  val testRec2 = TestBean(37L, "cat", TsVector("'ca'"), "fat")

  test("Text search Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        Tests.schema create,
        ///
        Tests forceInsertAll List(testRec1, testRec2)
      ).andThen(
        DBIO.seq(
          // get_current_ts_config
          Tests.filter(_.id === 33L).map(r => currTsConfig()).result.head.map(
            r => assert("english" === r)
          ),
          // @@
          Tests.filter(r => toTsVector(r.text, Some("english")) @@ tsQuery("cat & rat")).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1) === r)
          ),
          // '||' (concatenate)
          Tests.filter(r => (toTsVector(r.text) @+ toTsVector(r.comment)) @@ tsQuery("cat & fat".bind)).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2) === r)
          ),
          // &&
          Tests.filter(r => toTsVector(r.text) @@ (tsQuery("cat".bind) @& tsQuery("rat".bind))).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1) === r)
          ),
          // '||' (or)
          Tests.filter(r => toTsVector(r.text) @@ (tsQuery("cat".bind) @| tsQuery("rat".bind))).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2) === r)
          ),
          // !!
          Tests.filter(r => toTsVector(r.text) @@ (tsQuery("cat".bind) @& tsQuery("rat".bind).!!)).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          // @>
          Tests.filter(r => plainToTsQuery(r.text, Some("english")) @> toTsQuery(r.comment, Some("english"))).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1) === r)
          ),
          // length
          Tests.filter(_.id === 33L).map(r => toTsVector(r.text).length).result.head.map(
            r => assert(4 === r)
          ),
          // strip
          Tests.filter(_.id === 33L).map(r => toTsVector(r.text).strip).result.head.map(
            r => assert(TsVector("'ate' 'cat' 'fat' 'rat'") === r)
          ),
          // setweight
          Tests.filter(_.id === 33L).map(r => toTsVector(r.text).setWeight('A')).result.head.map(
            r => assert(TsVector("'ate':3A 'cat':2A 'fat':1A 'rat':4A") === r)
          ),
          // numnode
          Tests.filter(_.id === 33L).map(r => tsQuery("(fat & rat) | cat".bind.?).numNode).result.head.map(
            r => assert(5 === r)
          ),
          // querytree
          Tests.filter(_.id === 37L).map(r => toTsQuery(r.text).queryTree).result.head.map(
            r => assert("'cat'" === r)
          ),
          // ts_rewrite
          Tests.filter(_.id === 33L).map(r => tsQuery("a & b".bind.?).rewrite(tsQuery("a"), tsQuery("foo|bar"))).result.head.map(
            r => assert(TsQuery("'b' & ( 'foo' | 'bar' )") === r)
          ),
          Tests.filter(_.id === 33L).map(r => tsQuery("a & b").rewrite("select 'a'::tsquery, 'foo|bar'::tsquery")).result.head.map(
            r => assert(TsQuery("'b' & ( 'bar' | 'foo' )") === r)
          )
        )
      ).andFinally(
        Tests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  test("Text search Lifted support - others") {
    val query = tsQuery("neutrino|(dark & matter)".bind)

    Await.result(db.run(
      DBIO.seq(
        Tests.schema create,
        ///
        Tests.forceInsert(TestBean(11L, "Neutrinos in the Sun", TsVector("'at'"), "")),
        Tests.forceInsert(TestBean(12L, "The Sudbury Neutrino Detector", TsVector("'ss'"), "")),
        Tests.forceInsert(TestBean(13L, "A MACHO View of Galactic Dark Matter", TsVector("'at'"), "")),
        Tests.forceInsert(TestBean(14L, "Hot Gas and Dark Matter", TsVector("'at'"), "")),
        Tests.forceInsert(TestBean(15L, "The Virgo Cluster: Hot Plasma and Dark Matter", TsVector("'at'"), "")),
        Tests.forceInsert(TestBean(16L, "Rafting for Solar Neutrinos", TsVector("'at'"), "")),
        Tests.forceInsert(TestBean(17L, "NGC 4650A: Strange Galaxy and Dark Matter", TsVector("'ga'"), "")),
        Tests.forceInsert(TestBean(18L, "Hot Gas and Dark Matter", TsVector("'ga'"), "")),
        Tests.forceInsert(TestBean(19L, "Ice Fishing for Cosmic Neutrinos", TsVector("'fish'"), "")),
        Tests.forceInsert(TestBean(20L, "Weak Lensing Distorts the Universe", TsVector("'w'"), ""))
      ).andThen(
        DBIO.seq(
          // ts_rank
          Tests.filter(r => toTsVector(r.text) @@ query).map(r => (r.id, r.text, tsRank(toTsVector(r.text), query))).sortBy(_._3).result.map(
            _.map { r => println(s"${r._1}  |  ${r._2} | ${r._3}") }
          ),
          // ts_rank_cd
          Tests.filter(r => toTsVector(r.text) @@ query).map(r => (r.id, r.text, tsRankCD(toTsVector(r.text), query))).sortBy(_._3).result.map(
            _.map { r => println(s"${r._1}  |  ${r._2} | ${r._3}") }
          ),
          // ts_headline
          Tests.filter(r => toTsVector(r.text) @@ query).map(r => (r.id, r.text, tsHeadline(r.text, query, Some("english")))).result.map(
            _.map { r => println(s"${r._1}  |  ${r._2} | ${r._3}") }
          )
        )
      ).andFinally(
        Tests.schema drop
      ).transactionally
    ), Duration.Inf)

  }

  //--------------------------------------------------------------------------------

  test("Text search Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    case class SearchBean(id: Long, tVec: TsVector, tQ: TsQuery)

    implicit val getSearchBeanResult = GetResult(r => SearchBean(r.nextLong(), r.nextTsVector(), r.nextTsQuery))

    val b = SearchBean(101L, TsVector("'ate' 'cat' 'fat' 'rat'"), TsQuery("'rat'"))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table tsTestTable(
              id int8 not null primary key,
              tsvec tsvector not null,
              tsquery tsquery)
          """,
        ///
        sqlu""" insert into tsTestTable values(${b.id}, ${b.tVec}, ${b.tQ}) """,
        sql""" select * from tsTestTable where id = ${b.id} """.as[SearchBean].head.map(
          r => assert(b === r)
        ),
        ///
        sqlu"drop table if exists tsTestTable cascade"
      ).transactionally
    ), Duration.Inf)
  }
}