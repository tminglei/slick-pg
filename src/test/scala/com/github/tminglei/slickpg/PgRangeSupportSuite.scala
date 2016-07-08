package com.github.tminglei.slickpg

import java.sql.Timestamp
import org.scalatest.FunSuite
import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration._

class PgRangeSupportSuite extends FunSuite {
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  val tsFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def ts(str: String) = new Timestamp(tsFormatter.parse(str).getTime)

  case class RangeBean(
    id: Long,
    intRange: Range[Int],
    floatRange: Range[Float],
    tsRange: Option[Range[Timestamp]]
    )

  class RangeTestTable(tag: Tag) extends Table[RangeBean](tag, "RangeTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def intRange = column[Range[Int]]("int_range", O.Default(Range(3, 5)))
    def floatRange = column[Range[Float]]("float_range")
    def tsRange = column[Option[Range[Timestamp]]]("ts_range")

    def * = (id, intRange, floatRange, tsRange) <> (RangeBean.tupled, RangeBean.unapply)
  }
  val RangeTests = TableQuery[RangeTestTable]

  //-------------------------------------------------------------------------------

  val testRec1 = RangeBean(33L, Range(3, 5), Range(1.5f, 3.3f),
    Some(Range(ts("2010-01-01 14:30:00"), ts("2010-01-03 15:30:00"))))
  val testRec2 = RangeBean(35L, Range(31, 59), Range(11.5f, 33.3f),
    Some(Range(ts("2011-01-01 14:30:00"), ts("2011-11-01 15:30:00"))))
  val testRec3 = RangeBean(41L, Range.emptyRange[Int], Range(Some(7.5f), None, `[_,_)`), None)

  test("Range Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        RangeTests.schema create,
        ///
        RangeTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          RangeTests.sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // @>
          RangeTests.filter(_.tsRange @>^ ts("2011-10-01 15:30:00")).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          RangeTests.filter(_.floatRange @> Range(10.5f, 12f).bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec3) === r)
          ),
          // <@
          RangeTests.filter(ts("2011-10-01 15:30:00").bind <@^: _.tsRange).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          RangeTests.filter(Range(10.5f, 12f).bind <@: _.floatRange).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec3) === r)
          ),
          // &&
          RangeTests.filter(_.intRange @& Range(4,6).bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1) === r)
          ),
          // <<
          RangeTests.filter(_.intRange << Range(10, 15).bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1) === r)
          ),
          // >>
          RangeTests.filter(_.intRange >> Range(10, 15).bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          // &<
          RangeTests.filter(_.floatRange &< Range(2.9f, 7.7f).bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1) === r)
          ),
          // &>
          RangeTests.filter(_.floatRange &> Range(2.9f, 7.7f).bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2, testRec3) === r)
          ),
          // -|-
          RangeTests.filter(_.intRange -|- Range(5, 31).bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2) === r)
          ),
          // +
          RangeTests.filter(_.id === 41L).map(t => t.intRange + Range(3, 6).bind).result.head.map(
            r => assert(Range(3, 6) === r)
          ),
          // *
          RangeTests.filter(_.id === 33L).map(t => t.intRange * Range(3, 6).bind).result.head.map(
            r => assert(Range(3, 5) === r)
          ),
          // -
          RangeTests.filter(_.id === 33L).map(t => t.intRange - Range(3, 6).bind).result.head.map(
            r => assert(Range.emptyRange[Int] === r)
          ),
          // lower
          RangeTests.filter(_.id === 41L).map(t => t.intRange.lower.?).result.head.map(
            r => assert(None === r)
          ),
          // upper
          RangeTests.filter(_.id === 33L).map(t => t.intRange.upper).result.head.map(
            r => assert(5 === r)
          )
        )
      ).andFinally(
        RangeTests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  //////////////////////////////////////////////////////////////////////

  test("Range Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getRangeBeanResult = GetResult(r =>
      RangeBean(r.nextLong(), r.nextIntRange(), r.nextFloatRange(), r.nextTimestampRangeOption()))

    val b = RangeBean(34L, Range(3, 5), Range(1.5f, 3.3f), Some(Range(ts("2010-01-01 14:30:00"), ts("2010-01-03 15:30:00"))))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table RangeTest(
              id int8 not null primary key,
              int_range int4range not null,
              float_range numrange not null,
              ts_range tsrange)
          """,
        ///
        sqlu""" insert into RangeTest values(${b.id}, ${b.intRange}, ${b.floatRange}, ${b.tsRange}) """,
        sql""" select * from RangeTest where id = ${b.id} """.as[RangeBean].head.map(
          r => assert(b === r)
        ),
        ///
        sqlu"drop table if exists RangeTest cascade"
      ).transactionally
    ), Duration.Inf)
  }
}
