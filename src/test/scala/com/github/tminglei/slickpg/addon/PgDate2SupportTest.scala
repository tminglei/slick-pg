package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import java.time._
import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class PgDate2SupportTest {
  import slick.driver.PostgresDriver

  object MyPostgresDriver extends PostgresDriver
                            with PgDate2Support {

    override val api = new API with DateTimeImplicits

    ///
    val plainAPI = new API with Date2DateTimePlainImplicits
  }

  ///
  import MyPostgresDriver.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  case class DatetimeBean(
    id: Long,
    date: LocalDate,
    time: LocalTime,
    dateTime: LocalDateTime,
    dateTimeOffset: OffsetDateTime,
    dateTimeTz: ZonedDateTime,
    duration: Duration,
    period: Period,
    zone: ZoneId
    )

  class DatetimeTable(tag: Tag) extends Table[DatetimeBean](tag,"Datetime2Test") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def date = column[LocalDate]("date")
    def time = column[LocalTime]("time")
    def dateTime = column[LocalDateTime]("dateTime")
    def dateTimeOffset = column[OffsetDateTime]("dateTimeOffset")
    def dateTimeTz = column[ZonedDateTime]("dateTimeTz")
    def duration = column[Duration]("duration")
    def period = column[Period]("period")
    def zone = column[ZoneId]("zone")

    def * = (id, date, time, dateTime, dateTimeOffset, dateTimeTz, duration, period, zone) <>
            (DatetimeBean.tupled, DatetimeBean.unapply)
  }
  val Datetimes = TableQuery[DatetimeTable]

  //------------------------------------------------------------------------------

  val testRec1 = new DatetimeBean(101L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01.101357"),
    LocalDateTime.parse("2001-01-03T13:21:00.223571"),
    OffsetDateTime.parse("2001-01-03 13:21:00.102203+08", date2TzDateTimeFormatter),
    ZonedDateTime.parse("2001-01-03 13:21:00.102203+08", date2TzDateTimeFormatter),
    Duration.parse("P1DT1H1M0.335701S"), Period.parse("P1Y2M3W4D"), ZoneId.of("America/New_York"))
  val testRec2 = new DatetimeBean(102L, LocalDate.MAX, LocalTime.parse("03:14:07"),
    LocalDateTime.MAX, LocalDateTime.MAX.atOffset(ZoneOffset.UTC), LocalDateTime.MAX.atZone(ZoneId.of("UTC")),
    Duration.parse("P1587D"), Period.parse("P15M7D"), ZoneId.of("Europe/London"))
  val testRec3 = new DatetimeBean(103L, LocalDate.MIN, LocalTime.parse("11:13:34"),
    LocalDateTime.MIN, LocalDateTime.MIN.atOffset(ZoneOffset.UTC), LocalDateTime.MIN.atZone(ZoneId.of("UTC")),
    Duration.parse("PT63H16M2S"), Period.parse("P3M5D"), ZoneId.of("Asia/Shanghai"))

  @Test
  def testDatetimeFunctions(): Unit = {
    Await.result(db.run(DBIO.seq(
      sqlu"SET TIMEZONE TO '+8';",
      Datetimes.schema create,
      ///
      Datetimes forceInsertAll List(testRec1, testRec2, testRec3),
      // 0. simple test
      Datetimes.result.head.map(
        // testRec2 and testRec3 will fail to equal test, because of different time zone
        assertEquals(testRec1/*List(testRec1, testRec2, testRec3)*/, _)
      ),
      Datetimes.filter(_.id === 101L.bind).map { r => (r.date.isFinite, r.dateTime.isFinite, r.duration.isFinite) }.result.head.map(
        assertEquals((true, true, true), _)
      ),
      Datetimes.filter(_.id === 102L.bind).map {
        r => (r.date.isFinite, r.dateTime.isFinite, r.dateTimeOffset.isFinite, r.dateTimeTz.isFinite)
      }.result.head.map(
        assertEquals((false, false, false, false), _)
      ),
      Datetimes.filter(_.id === 103L.bind).map {
        r => (r.date.isFinite, r.dateTime.isFinite, r.dateTimeOffset.isFinite, r.dateTimeTz.isFinite)
      }.result.head.map(
        assertEquals((false, false, false, false), _)
      ),
      // 1. '+'
      Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time).result.head.map(
        assertEquals(LocalDateTime.parse("2010-11-03T12:33:01.101357"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date).result.head.map(
        assertEquals(LocalDateTime.parse("2010-11-03T12:33:01.101357"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date +++ r.duration).result.head.map(
        assertEquals(LocalDateTime.parse("2010-11-04T01:01:00.335701"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time +++ r.duration).result.head.map(
        assertEquals(LocalTime.parse("13:34:01.437058"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.dateTime +++ r.duration).result.head.map(
        assertEquals(LocalDateTime.parse("2001-01-04T14:22:00.559272"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind).result.head.map(
        assertEquals(LocalDate.parse("2010-11-10"), _)
      ),
      // 2. '-'
      Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind).result.head.map(
        assertEquals(LocalDate.parse("2010-11-02"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.dateTime -- r.time).result.head.map(
        assertEquals(LocalDateTime.parse("2001-01-03T00:47:59.122214"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.dateTime - r.date).result.head.map(
        assertEquals(Duration.parse("-P3590DT10H39M").plus(Duration.parse("PT0.223571S")), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date.asColumnOf[LocalDateTime] - r.dateTime).result.head.map(
        assertEquals(Duration.parse("P3590DT10H38M59.776429S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date - LocalDate.parse("2009-07-05")).result.head.map(
        assertEquals(486, _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time - LocalTime.parse("02:37:00").bind).result.head.map(
        assertEquals(Duration.parse("PT9H56M1.101357S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.dateTime --- r.duration).result.head.map(
        assertEquals(LocalDateTime.parse("2001-01-02T12:19:59.887870"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.time --- r.duration).result.head.map(
        assertEquals(LocalTime.parse("11:32:00.765656"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.date --- r.duration).result.head.map(
        assertEquals(LocalDateTime.parse("2010-11-01T22:58:59.664299"), _)
      ),
      // 3. 'age'
      Datetimes.filter(_.id === 101L.bind).map(r => r.dateTime.age === r.dateTime.age(Functions.currentDate.asColumnOf[LocalDateTime])).result.head.map(
        assertEquals(true, _)
      ),
      // 4. 'part'
      Datetimes.filter(_.id === 101L.bind).map(r => r.dateTime.part("year")).result.head.map(
        assertEquals(2001, _, 0.00001d)
      ),
      Datetimes.filter(_.id === 102L.bind).map(r => r.duration.part("year")).result.head.map(
        assertEquals(0, _, 0.00001d)
      ),
      // 5. 'trunc'
      Datetimes.filter(_.id === 101L.bind).map(r => r.dateTime.trunc("day")).result.head.map(
        assertEquals(LocalDateTime.parse("2001-01-03T00:00:00"), _)
      ),
      // 6. interval
      Datetimes.filter(_.id === 101L.bind).map(r => r.duration + Duration.parse("PT3H").bind).result.head.map(
        assertEquals(Duration.parse("P1DT4H1M0.335701S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.duration + Period.of(0, 0, 3).bind.toDuration).result.head.map(
        assertEquals(Duration.parse("PT97H1M0.335701S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => -r.duration).result.head.map(
        assertEquals(Duration.parse("-P1DT1H1M0.335701S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.duration - Duration.parse("PT2H").bind).result.head.map(
        assertEquals(Duration.parse("P1DT-1H1M0.335701S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.duration * 3.5).result.head.map(
        assertEquals(Duration.parse("P3DT15H33M31.174954S"), _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.duration / 5.0).result.head.map(
        assertEquals(Duration.parse("PT5H12.06714S"), _)
      ),
      Datetimes.filter(_.id === 102L.bind).map(r => r.duration.justifyDays).result.head.map(
        assertEquals(Duration.parse("P1587D"), _)
      ),
      Datetimes.filter(_.id === 103L.bind).map(r => r.duration.justifyHours).result.head.map(
        assertEquals(Duration.parse("P2DT15H16M2S"), _)
      ),
      Datetimes.filter(_.id === 103L.bind).map(r => r.duration.justifyInterval).result.head.map(
        assertEquals(Duration.parse("P2DT15H16M2S"), _)
      ),
      // 7. timestamp with time zone
      Datetimes.filter(_.id === 101L.bind).map(r => r.dateTimeTz.age === r.dateTimeTz.age(Functions.currentDate.asColumnOf[ZonedDateTime])).result.head.map(
        assertEquals(true, _)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.dateTimeTz.part("year")).result.head.map(
        assertEquals(2001, _, 0.00001d)
      ),
      Datetimes.filter(_.id === 101L.bind).map(r => r.dateTimeTz.trunc("day")).result.head.map(
        assertEquals(ZonedDateTime.parse("2001-01-03 00:00:00+08", date2TzDateTimeFormatter), _)
      ),
      // 8. Timezones
      Datetimes.filter(_.id === 101L.bind).map(r => r.zone).result.head.map(
        assertEquals(ZoneId.of("America/New_York"), _)
      ),
      // 9. +/-infinity
      Datetimes.filter(_.id === 102L.bind).map { r => (r.date, r.dateTime, r.dateTimeOffset, r.dateTimeTz) }.result.head.map(
        assertEquals((LocalDate.MAX, LocalDateTime.MAX,
          LocalDateTime.MAX.atOffset(ZoneOffset.UTC), LocalDateTime.MAX.atZone(ZoneId.of("UTC"))), _)
      ),
      Datetimes.filter(_.id === 103L.bind).map { r => (r.date, r.dateTime, r.dateTimeOffset, r.dateTimeTz) }.result.head.map(
        assertEquals((LocalDate.MIN, LocalDateTime.MIN,
          LocalDateTime.MIN.atOffset(ZoneOffset.UTC), LocalDateTime.MIN.atZone(ZoneId.of("UTC"))), _)
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
      r.nextLong(), r.nextLocalDate(), r.nextLocalTime(), r.nextLocalDateTime(), r.nextOffsetDateTime(), r.nextZonedDateTime(),
      r.nextDuration(), r.nextPeriod(), r.nextZoneId))

    val b = new DatetimeBean(107L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01.101357"),
      LocalDateTime.parse("2001-01-03T13:21:00.223571"),
      OffsetDateTime.parse("2001-01-03 13:21:00.102203+08", date2TzDateTimeFormatter),
      ZonedDateTime.parse("2001-01-03 13:21:00.102203+08", date2TzDateTimeFormatter),
      Duration.parse("P1DT1H1M0.335701S"), Period.parse("P1Y2M3W4D"), ZoneId.of("Africa/Johannesburg"))

    Await.result(db.run(DBIO.seq(
      sqlu"""create table Datetime2Test(
              id int8 not null primary key,
              date date not null,
              time time not null,
              ts timestamp not null,
              tsos timestamptz not null,
              tstz timestamptz not null,
              duration interval not null,
              period interval not null,
              zone text not null)
          """,
      sqlu"SET TIMEZONE TO '+8';",
      ///
      sqlu"insert into Datetime2Test values(${b.id}, ${b.date}, ${b.time}, ${b.dateTime}, ${b.dateTimeOffset}, ${b.dateTimeTz}, ${b.duration}, ${b.period}, ${b.zone})",
      sql"select * from Datetime2Test where id = ${b.id}".as[DatetimeBean].head.map(
        assertEquals(b, _)
      ),
      ///
      sqlu"drop table if exists Datetime2Test cascade"
    ).transactionally), concurrent.duration.Duration.Inf)
  }
}
