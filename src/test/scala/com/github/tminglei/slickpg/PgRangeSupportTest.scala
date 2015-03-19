package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import java.sql.Timestamp
import slick.jdbc.{SetParameter, PositionedParameters, GetResult}

import scala.concurrent.ExecutionContext.Implicits.global

class PgRangeSupportTest {
  import MyPostgresDriver.api._
  import MyPostgresDriver.MappedJdbcType

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  val tsFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def ts(str: String) = new Timestamp(tsFormatter.parse(str).getTime)

  case class CustomRange(
    from:Timestamp, 
    to:Timestamp)
  object CustomRange {
    def apply(r: Range[Timestamp]): CustomRange = CustomRange(r.start, r.end)
  }

  case class RangeBean(
    id: Long,
    intRange: Range[Int],
    floatRange: Range[Float],
    tsRange: Option[Range[Timestamp]],
    customRange: CustomRange)

  implicit val CustomRangeType = MappedColumnType.base[CustomRange, Range[Timestamp]](
    { case CustomRange(from, to) => Range(from, to, `[_,_)`) },
    { case Range(from, to, `[_,_)`) => CustomRange(from, to) })
    // MappedColumnType.base returns a 'BaseColumnType[T]', but it's actually a 'MappedJdbcType[T, U] with BaseTypedType[T]'
    .asInstanceOf[MappedJdbcType[CustomRange, Range[Timestamp]]]

  class RangeTestTable(tag: Tag) extends Table[RangeBean](tag, "RangeTest") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def intRange = column[Range[Int]]("int_range", O.Default(Range(3, 5)))
    def floatRange = column[Range[Float]]("float_range")
    def tsRange = column[Option[Range[Timestamp]]]("ts_range")
    def customRange = column[CustomRange]("custom_range")

    def * = (id, intRange, floatRange, tsRange, customRange) <> (RangeBean.tupled, RangeBean.unapply)
  }
  val RangeTests = TableQuery[RangeTestTable]

  //-------------------------------------------------------------------------------

  val testRec1 = RangeBean(33L, Range(3, 5), Range(1.5f, 3.3f),
    Some(Range(ts("2010-01-01 14:30:00"), ts("2010-01-03 15:30:00"))),
    CustomRange(ts("2010-01-01 14:30:00"), ts("2010-03-01 14:30:00")))
  val testRec2 = RangeBean(35L, Range(31, 59), Range(11.5f, 33.3f),
    Some(Range(ts("2011-01-01 14:30:00"), ts("2011-11-01 15:30:00"))),
    CustomRange(ts("2011-01-01 14:30:00"), ts("2011-03-01 14:30:00")))
  val testRec3 = RangeBean(41L, Range(1, 5), Range(7.5f, 15.3f), None,
    CustomRange(ts("2012-01-01 14:30:00"), ts("2012-03-01 14:30:00")))

  @Test
  def testRangeFunctions(): Unit = {
    db.run(DBIO.seq(
      RangeTests.schema create,
      ///
      RangeTests forceInsertAll List(testRec1, testRec2, testRec3),
      // 0. '@>'/'<@'/'&&'
      RangeTests.filter(_.tsRange @>^ ts("2011-10-01 15:30:00")).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec2), _)
      ),
      RangeTests.filter(_.customRange @>^ ts("2011-10-01 15:30:00")).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec2, testRec3), _)
      ),
      RangeTests.filter(ts("2011-10-01 15:30:00").bind <@^: _.tsRange).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec2), _)
      ),
      RangeTests.filter(_.floatRange @> Range(10.5f, 12f).bind).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec3), _)
      ),
      RangeTests.filter(Range(10.5f, 12f).bind <@: _.floatRange).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec3), _)
      ),
      RangeTests.filter(_.intRange @& Range(4,6).bind).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec1, testRec3), _)
      ),
      // 2. '<<'/'>>'
      RangeTests.filter(_.intRange << Range(10, 15).bind).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec1, testRec3), _)
      ),
      RangeTests.filter(_.intRange >> Range(10, 15).bind).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec2), _)
      ),
      // 3. '&<'/'&>'/'-|-'
      RangeTests.filter(_.floatRange &< Range(2.9f, 7.7f).bind).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec1), _)
      ),
      RangeTests.filter(_.floatRange &> Range(2.9f, 7.7f).bind).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec2, testRec3), _)
      ),
      RangeTests.filter(_.intRange -|- Range(5, 31).bind).sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec1, testRec2, testRec3), _)
      ),
      // 4. '+'/'*'/'-'
      RangeTests.filter(_.id === 41L).map(t => t.intRange + Range(3, 6).bind).result.head.map(
        assertEquals(Range(1, 6), _)
      ),
      RangeTests.filter(_.id === 41L).map(t => t.intRange * Range(3, 6).bind).result.head.map(
        assertEquals(Range(3, 5), _)
      ),
      RangeTests.filter(_.id === 41L).map(t => t.intRange - Range(3, 6).bind).result.head.map(
        assertEquals(Range(1, 3), _)
      ),
      // 5. 'lower'/'upper'
      RangeTests.filter(_.id === 41L).map(t => t.intRange.lower).result.head.map(
        assertEquals(1, _)
      ),
      RangeTests.filter(_.id === 41L).map(t => t.intRange.upper).result.head.map(
        assertEquals(5, _)
      ),
      ///
      RangeTests.schema drop
    ).transactionally)
  }

  //////////////////////////////////////////////////////////////////////

  @Test
  def testPlainRangeFunctions(): Unit = {
    import MyPlainPostgresDriver.plainAPI._

    implicit val getRangeBeanResult = GetResult(r =>
      RangeBean(r.nextLong(), r.nextIntRange(), r.nextFloatRange(), r.nextTimestampRangeOption(), CustomRange(r.nextTimestampRange())))

    val b = RangeBean(33L, Range(3, 5), Range(1.5f, 3.3f), 
                      Some(Range(ts("2010-01-01 14:30:00"), ts("2010-01-03 15:30:00"))),
                      CustomRange(ts("2015-01-01 14:30:00"), ts("2015-03-01 14:30:00")))

    implicit object SetCustomRange extends SetParameter[CustomRange] {
      def apply(v: CustomRange, pp: PositionedParameters) = 
        pp.setObject(utils.mkPGobject("daterange", v.toString), java.sql.Types.OTHER)
    }

    db.run(DBIO.seq(
      sqlu"""create table RangeTest(
            |  id int8 not null primary key,
            |  int_range int4range not null,
            |  float_range numrange not null,
            |  ts_range tsrange,
            |  custom_range tsrange not null)
          """,
      ///
      sqlu"insert into RangeTest values(${b.id}, ${b.intRange}, ${b.floatRange}, ${b.tsRange}, ${b.customRange})",
      sql"select * from RangeTest where id = ${b.id}".as[RangeBean].head.map(
        assertEquals(b, _)
      ),
      ///
      sqlu"drop table if exists RangeTest cascade"
    ).transactionally)
  }
}
