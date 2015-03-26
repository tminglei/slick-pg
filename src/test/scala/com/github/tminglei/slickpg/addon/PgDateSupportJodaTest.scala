package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import org.joda.time._
import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class PgDateSupportJodaTest {
  import slick.driver.PostgresDriver

  object MyPostgresDriver extends PostgresDriver
                            with PgDateSupportJoda {

    override val api = new API with DateTimeImplicits

    ///
    val plainAPI = new API with JodaDateTimePlainImplicits
  }

  ///
  import MyPostgresDriver.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  case class DatetimeBean(
    id: Long,
    date: LocalDate,
    time: LocalTime,
    dateTime: LocalDateTime,
    dateTimetz: DateTime,
    interval: Period
    )

  class DatetimeTable(tag: Tag) extends Table[DatetimeBean](tag, "DatetimeJodaTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def date = column[LocalDate]("date")
    def time = column[LocalTime]("time")
    def datetime = column[LocalDateTime]("datetime")
    def datetimetz = column[DateTime]("datetimetz")
    def interval = column[Period]("interval")

    def * = (id, date, time, datetime, datetimetz, interval) <> (DatetimeBean.tupled, DatetimeBean.unapply)
  }
  val Datetimes = TableQuery[DatetimeTable]

  //------------------------------------------------------------------------------

  val testRec1 = new DatetimeBean(101L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01.101357"),
    LocalDateTime.parse("2001-01-03T13:21:00.223571"), DateTime.parse("2001-01-03 13:21:00.102203+08", jodaTzDateTimeFormatter),
    Period.parse("P1DT1H1M0.335701S"))
  val testRec2 = new DatetimeBean(102L, LocalDate.parse("2011-03-02"), LocalTime.parse("03:14:07"),
    LocalDateTime.parse("2012-05-08T11:31:06"), DateTime.parse("2012-05-08 11:31:06.113-05", jodaTzDateTimeFormatter),
    Period.parse("P1587D"))
  val testRec3 = new DatetimeBean(103L, LocalDate.parse("2000-05-19"), LocalTime.parse("11:13:34"),
    LocalDateTime.parse("2019-11-03T13:19:03"), DateTime.parse("2019-11-03 13:19:03.000+03", jodaTzDateTimeFormatter),
    Period.parse("PT63H16M2S"))

  @Test
  def testDatetimeFunctions(): Unit = {
    val now = LocalDateTime.now
    val now1 = DateTime.now
    val now2 = LocalTime.now

    Await.result(db.run(DBIO.seq(
      Datetimes.schema create,
      ///
      sqlu"SET TIMEZONE TO '+8';",
      Datetimes forceInsertAll List(testRec1, testRec2, testRec3),
      // 0. simple test
      Datetimes.result.head.map(
        // testRec2 and testRec3 will fail to equal test, because of different time zone
        assertEquals(testRec1/*List(testRec1, testRec2, testRec3)*/, _)
      ),
      Datetimes.filter(_.id === 101L.bind).map { r => (r.date.isFinite, r.datetime.isFinite, r.interval.isFinite) }.result.head.map(
        assertEquals((true, true, true), _)
      ),
      // 1. '+'
      Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time).result.head.map(
        assertEquals(LocalDateTime.parse("2010-11-03T12:33:01.101357"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date).result.head.map(
        assertEquals(LocalDateTime.parse("2010-11-03T12:33:01.101"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date +++ r.interval).result.head.map(
        assertEquals(LocalDateTime.parse("2010-11-04T01:01:00.335"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time +++ r.interval).result.head.map(
        assertEquals(LocalTime.parse("13:34:01.436"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.datetime +++ r.interval).result.head.map(
        assertEquals(LocalDateTime.parse("2001-01-04T14:22:00.558"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind).result.head.map(
        assertEquals(LocalDate.parse("2010-11-10"), _)
      ),
      // 2. '-'
      Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind).result.head.map(
        assertEquals(LocalDate.parse("2010-11-02"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.datetime -- r.time).result.head.map(
        assertEquals(LocalDateTime.parse("2001-01-03T00:47:59.122"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.datetime - r.date).result.head.map(
        assertEquals(Period.parse("P-3590DT-10H-38M-60S").plus(Period.parse("PT0.222S")), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date.asColumnOf[LocalDateTime] - r.datetime).result.head.map(
        assertEquals(Period.parse("P3590DT10H38M59.777S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date - LocalDate.parse("2009-07-05")).result.head.map(
        assertEquals(486, _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time - LocalTime.parse("02:37:00").bind).result.head.map(
        assertEquals(Period.parse("PT9H56M1.100S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.datetime --- r.interval).result.head.map(
        assertEquals(LocalDateTime.parse("2001-01-02T12:19:59.888"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time --- r.interval).result.head.map(
        assertEquals(LocalTime.parse("11:32:00.766"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date --- r.interval).result.head.map(
        assertEquals(LocalDateTime.parse("2010-11-01T22:58:59.665"), _)
      ),
      // 3. 'age'/'part'/'trunc'
      Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.age === r.datetime.age(Functions.currentDate.asColumnOf[LocalDateTime])).result.head.map(
        assertEquals(true, _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.part("year")).result.head.map(
        assertEquals(2001, _, 0.00001d)
      ),
      Datetimes.filter(_.id === 102L.bind).map(r => r.interval.part("year")).result.head.map(
        assertEquals(0, _, 0.00001d)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.trunc("day")).result.head.map(
        assertEquals(LocalDateTime.parse("2001-01-03T00:00:00"), _)
      ),
      // 4. interval
      Datetimes.filter(_.id === 101L.bind).map(r => r.interval + Period.parse("PT3H").bind).result.head.map(
        assertEquals(Period.parse("P1DT4H1M0.335S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => -r.interval).result.head.map(
        assertEquals(Period.parse("P-1DT-1H-1M-1S").plus(Period.parse("PT0.665S")), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.interval - Period.parse("PT2H").bind).result.head.map(
        assertEquals(Period.parse("P1DT-58M-60S").plus(Period.parse("PT0.335S")), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.interval * 3.5).result.head.map(
        assertEquals(Period.parse("P3DT15H33M31.172S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.interval / 5.0).result.head.map(
        assertEquals(Period.parse("PT5H12.067S"), _)
      ),
      Datetimes.filter(_.id === 102L.bind).map(r => r.interval.justifyDays).result.head.map(
        assertEquals(Period.parse("P4Y4M27D"), _)
      ),
      Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyHours).result.head.map(
        assertEquals(Period.parse("P2DT15H16M2S"), _)
      ),
      Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyInterval).result.head.map(
        assertEquals(Period.parse("P2DT15H16M2S"), _)
      ),
      // 5. timestamp with time zone
      Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.age === r.datetimetz.age(Functions.currentDate.asColumnOf[DateTime])).result.head.map(
        assertEquals(true, _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.part("year")).result.head.map(
        assertEquals(2001, _, 0.00001d)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.trunc("day")).result.head.map(
        assertEquals(DateTime.parse("2001-01-03 00:00:00.000+08", jodaTzDateTimeFormatter), _)
      ),
      // 6. update and check
      Datetimes.filter(_.id === 101L.bind).map(_.datetime).update(now),
      Datetimes.filter(_.id === 101L.bind).map(_.datetime).result.head.map(
        assertEquals(now, _)
      ),
      //
      Datetimes.filter(_.id === 101L.bind).map(_.datetimetz).update(now1),
      Datetimes.filter(_.id === 101L.bind).map(_.datetimetz).result.head.map(
        assertEquals(now1, _)
      ),
      //
      Datetimes.filter(_.id === 101L.bind).map(_.time).update(now2),
      Datetimes.filter(_.id === 101L.bind).map(_.time).result.head.map(
        assertEquals(now2, _)
      ),
      ///
      Datetimes.schema drop
    ).transactionally), concurrent.duration.Duration.Inf)
  }

  //////////////////////////////////////////////////////////////////////

  @Test
  def testPlainDateFunctions(): Unit = {
    import MyPostgresDriver.plainAPI._

    implicit val getDateBean = GetResult(r => DatetimeBean(
      r.nextLong(), r.nextLocalDate(), r.nextLocalTime(), r.nextLocalDateTime(), r.nextZonedDateTime(), r.nextPeriod()))

    val b1 = new DatetimeBean(107L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01.101357"),
      LocalDateTime.parse("2001-01-03T13:21:00.223571"), DateTime.parse("2001-01-03 13:21:00.102203+08", jodaTzDateTimeFormatter),
      Period.parse("P1DT1H1M0.335701S"))
    val b2 = new DatetimeBean(108L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01"),
      LocalDateTime.parse("2001-01-03T13:21:00"), DateTime.parse("2001-01-03 13:21:00+08", jodaTzDateTimeFormatter_NoFraction),
      Period.parse("P1DT1H1M0.335701S"))

    Await.result(db.run(DBIO.seq(
      sqlu"""create table DatetimeJodaTest(
              id int8 not null primary key,
              date date not null,
              time time not null,
              ts timestamp not null,
              tstz timestamptz not null,
              period interval not null)
          """,
      sqlu"SET TIMEZONE TO '+8';",
      ///
      sqlu"insert into DatetimeJodaTest values(${b1.id}, ${b1.date}, ${b1.time}, ${b1.dateTime}, ${b1.dateTimetz}, ${b1.interval})",
      sql"select * from DatetimeJodaTest where id = ${b1.id}".as[DatetimeBean].head.map(
        assertEquals(b1, _)
      ),
      sqlu"insert into DatetimeJodaTest values(${b2.id}, ${b2.date}, ${b2.time}, ${b2.dateTime}, ${b2.dateTimetz}, ${b2.interval})",
      sql"select * from DatetimeJodaTest where id = ${b2.id}".as[DatetimeBean].head.map(
        assertEquals(b2, _)
      ),
      ///
      sqlu"drop table if exists DatetimeJodaTest cascade"
    ).transactionally), concurrent.duration.Duration.Inf)
  }
}
