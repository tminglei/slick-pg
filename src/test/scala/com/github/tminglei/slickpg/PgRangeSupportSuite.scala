package com.github.tminglei.slickpg

import java.sql.Timestamp
import java.time.{LocalDate, LocalDateTime, OffsetDateTime}

import org.scalatest.funsuite.AnyFunSuite
import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration._

class PgRangeSupportSuite extends AnyFunSuite with PostgresContainer {
  import MyPostgresProfile.api._

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  val tsFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def ts(str: String) = new Timestamp(tsFormatter.parse(str).getTime)
  def ldt(str: String) = LocalDateTime.parse(str)
  def odt(str: String) = OffsetDateTime.parse(str)
  def ld(str: String) = LocalDate.parse(str)

  case class RangeBean(
    id: Long,
    intRange: Range[Int],
    floatRange: Range[Float],
    tsRange: Option[Range[Timestamp]],
    ldtRange: Option[Range[LocalDateTime]],
    odtRange: Option[Range[OffsetDateTime]],
    ldRange: Option[Range[LocalDate]]
  )

  class RangeTestTable(tag: Tag) extends Table[RangeBean](tag, "RangeTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def intRange = column[Range[Int]]("int_range", O.Default(Range(3, 5)))
    def floatRange = column[Range[Float]]("float_range")
    def tsRange = column[Option[Range[Timestamp]]]("ts_range")
    def ldtRange = column[Option[Range[LocalDateTime]]]("ldt_range")
    def odtRange = column[Option[Range[OffsetDateTime]]]("odt_range")
    def ldRange = column[Option[Range[LocalDate]]]("ld_range")

    def * = (id, intRange, floatRange, tsRange, ldtRange, odtRange, ldRange) <> ((RangeBean.apply _).tupled, RangeBean.unapply)
  }
  val RangeTests = TableQuery[RangeTestTable]

  //-------------------------------------------------------------------------------

  val testRec1 = RangeBean(33L, Range(3, 5), Range(1.5f, 3.3f),
    Some(Range(ts("2010-01-01 14:30:00"), ts("2010-01-03 15:30:00"))),
    Some(Range(ldt("2010-01-01T14:30:00"), ldt("2010-01-03T15:30:00"))),
    Some(Range(odt("2010-01-01T14:30:00Z"), odt("2010-01-03T15:30:00Z"))),
    Some(Range(ld("2010-01-01"), ld("2010-01-03")))
  )
  val testRec2 = RangeBean(35L, Range(31, 59), Range(11.5f, 33.3f),
    Some(Range(ts("2011-01-01 14:30:00"), ts("2011-11-01 15:30:00"))),
    Some(Range(ldt("2011-01-01T14:30:00"), ldt("2011-11-01T15:30:00"))),
    Some(Range(odt("2011-01-01T14:30:00Z"), odt("2011-11-01T15:30:00Z"))),
    Some(Range(ld("2011-01-01"), ld("2011-11-01")))
  )
  val testRec3 = RangeBean(41L, Range.emptyRange[Int], Range(Some(7.5f), None, `[_,_)`), None, None, None, None)

  test("Range Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        RangeTests.schema create,
        ///
        RangeTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          sqlu"""SET TIME ZONE 'UTC'""",
          RangeTests.sortBy(_.id).to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // @>
          RangeTests.filter(_.tsRange @>^ ts("2011-10-01 15:30:00")).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          RangeTests.filter(_.ldtRange @>^ ldt("2011-10-01T15:30:00")).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          RangeTests.filter(_.odtRange @>^ odt("2011-10-01T15:30:00Z")).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          RangeTests.filter(_.ldRange @>^ ld("2011-10-01")).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          RangeTests.filter(_.floatRange @> Range(10.5f, 12f).bind).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec3) === r)
          ),
          // <@
          RangeTests.filter(ts("2011-10-01 15:30:00").bind <@^: _.tsRange).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          RangeTests.filter(ldt("2011-10-01T15:30:00").bind <@^: _.ldtRange).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          RangeTests.filter(odt("2011-10-01T15:30:00Z").bind <@^: _.odtRange).sortBy(_.id).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          RangeTests.filter(ld("2011-10-01").bind <@^: _.ldRange).sortBy(_.id).to[List].result.map(
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

    implicit val getRangeBeanResult: GetResult[RangeBean] = GetResult(r =>
      RangeBean(r.nextLong(), r.nextIntRange(), r.nextFloatRange(), r.nextTimestampRangeOption(),
        r.nextLocalDateTimeRangeOption(), r.nextOffsetDateTimeRangeOption(), r.nextLocalDateRangeOption()))

    val b = RangeBean(34L, Range(3, 5), Range(1.5f, 3.3f),
      Some(Range(ts("2010-01-01 14:30:00"), ts("2010-01-03 15:30:00"))),
      Some(Range(ldt("2010-01-01T14:30:00"), ldt("2010-01-03T15:30:00"))),
      Some(Range(odt("2010-01-01T14:30:00Z"), odt("2010-01-03T15:30:00Z"))),
      Some(Range(ld("2010-01-01"), ld("2010-01-03"))))

    Await.result(db.run(
      DBIO.seq(
        sqlu"""SET TIME ZONE 'UTC'""",
        sqlu"""create table RangeTest(
              id int8 not null primary key,
              int_range int4range not null,
              float_range numrange not null,
              ts_range tsrange,
              ldt_range tsrange,
              odt_range tstzrange,
              ld_range daterange
              )
          """,
        ///
        sqlu""" insert into RangeTest values(${b.id}, ${b.intRange}, ${b.floatRange}, ${b.tsRange}, ${b.ldtRange}, ${b.odtRange}, ${b.ldRange}) """,
        sql""" select * from RangeTest where id = ${b.id} """.as[RangeBean].head.map(
          r => assert(b === r)
        ),
        ///
        sqlu"drop table if exists RangeTest cascade"
      ).transactionally
    ), Duration.Inf)
  }
}
