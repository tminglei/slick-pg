package com.github.tminglei.slickpg

import java.util.concurrent.Executors

import org.scalatest.FunSuite
import org.threeten.bp._
import slick.jdbc.{GetResult, PostgresProfile}

import scala.concurrent.{Await, ExecutionContext}

class PgDate2bpSupportSuite extends FunSuite {
  implicit val testExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  object MyPostgresProfile extends PostgresProfile
                            with PgDateSupport2bp {

    override val api = new API with DateTimeImplicits

    ///
    val plainAPI = new API with BpDateTimePlainImplicits
  }

  ///
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class DatetimeBean(
    id: Long,
    date: LocalDate,
    time: LocalTime,
    dateTime: LocalDateTime,
    dateTimetz: ZonedDateTime,
    duration: Duration,
    period: Period
    )

  class DatetimeTable(tag: Tag) extends Table[DatetimeBean](tag,"DatetimebpTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def date = column[LocalDate]("date")
    def time = column[LocalTime]("time")
    def datetime = column[LocalDateTime]("datetime")
    def datetimeTz = column[ZonedDateTime]("datetimeTz")
    def duration = column[Duration]("duration")
    def period = column[Period]("period")

    def * = (id, date, time, datetime, datetimeTz, duration, period) <> (DatetimeBean.tupled, DatetimeBean.unapply)
  }
  val Datetimes = TableQuery[DatetimeTable]

  //------------------------------------------------------------------------------

  val testRec1 = new DatetimeBean(101L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01.101357"),
    LocalDateTime.parse("2001-01-03T13:21:00.223571"), ZonedDateTime.parse("2001-01-03 13:21:00.102203+08", bpTzDateTimeFormatter),
    Duration.parse("P1DT1H1M0.335701S"), Period.parse("P1Y2M3W4D"))
  val testRec2 = new DatetimeBean(102L, LocalDate.parse("2011-03-02"), LocalTime.parse("03:14:07"),
    LocalDateTime.parse("2012-05-08T11:31:06"), ZonedDateTime.parse("2012-05-08 11:31:06-05", bpTzDateTimeFormatter),
    Duration.parse("P1587D"), Period.parse("P15M7D"))
  val testRec3 = new DatetimeBean(103L, LocalDate.parse("2000-05-19"), LocalTime.parse("11:13:34"),
    LocalDateTime.parse("2019-11-03T13:19:03"), ZonedDateTime.parse("2019-11-03 13:19:03+03", bpTzDateTimeFormatter),
    Duration.parse("PT63H16M2S"), Period.parse("P3M5D"))

  test("Threeten date Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        sqlu"SET TIMEZONE TO '+8';",
        Datetimes.schema create,
        ///
        Datetimes forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          Datetimes.result.head.map(
            // testRec2 and testRec3 will fail to equal test, because of different time zone
            r => assert(testRec1/*List(testRec1, testRec2, testRec3)*/ === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map { r => (r.date.isFinite, r.datetime.isFinite, r.duration.isFinite) }.result.head.map(
            r => assert((true, true, true) === r)
          ),
          // +
          Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time).result.head.map(
            r => assert(LocalDateTime.parse("2010-11-03T12:33:01.101357") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date).result.head.map(
            r => assert(LocalDateTime.parse("2010-11-03T12:33:01.101357") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date +++ r.duration).result.head.map(
            r => assert(LocalDateTime.parse("2010-11-04T01:01:00.335701") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time +++ r.duration).result.head.map(
            r => assert(LocalTime.parse("13:34:01.437058") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime +++ r.duration).result.head.map(
            r => assert(LocalDateTime.parse("2001-01-04T14:22:00.559272") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind).result.head.map(
            r => assert(LocalDate.parse("2010-11-10") === r)
          ),
          // -
          Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind).result.head.map(
            r => assert(LocalDate.parse("2010-11-02") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime -- r.time).result.head.map(
            r => assert(LocalDateTime.parse("2001-01-03T00:47:59.122214") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime - r.date).result.head.map(
            r => assert(Duration.parse("-P3590DT10H39M").plus(Duration.parse("PT0.223571S")) === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date.asColumnOf[LocalDateTime] - r.datetime).result.head.map(
            r => assert(Duration.parse("P3590DT10H38M59.776429S") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date - LocalDate.parse("2009-07-05")).result.head.map(
            r => assert(486 === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time - LocalTime.parse("02:37:00").bind).result.head.map(
            r => assert(Duration.parse("PT9H56M1.101357S") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime --- r.duration).result.head.map(
            r => assert(LocalDateTime.parse("2001-01-02T12:19:59.887870") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time --- r.duration).result.head.map(
            r => assert(LocalTime.parse("11:32:00.765656") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date --- r.duration).result.head.map(
            r => assert(LocalDateTime.parse("2010-11-01T22:58:59.664299") === r)
          ),
          // age
//          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.age === r.datetime.age(Functions.currentDate.asColumnOf[LocalDateTime])).result.head.map(
//            r => assert(true === r)
//          ),
          // part
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.part("year")).result.head.map(
            r => assert(Math.abs(2001 - r) < 0.00001d)
          ),
          Datetimes.filter(_.id === 102L.bind).map(r => r.duration.part("year")).result.head.map(
            r => assert(Math.abs(0 - r) < 0.00001d)
          ),
          // trunc
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.trunc("day")).result.head.map(
            r => assert(LocalDateTime.parse("2001-01-03T00:00:00") === r)
          ),
          // isFinite
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.isFinite).result.head.map(
            r => assert(true === r)
          ),
          // at time zone
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetimeTz.atTimeZone("MST")).result.head.map(
            r => assert(r.isInstanceOf[LocalDateTime])
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time.atTimeZone("MST")).result.head.map(
            r => assert(r.isInstanceOf[OffsetTime])
          ),
          // interval
          DBIO.seq(
            // +
            Datetimes.filter(_.id === 101L.bind).map(r => r.duration + Duration.parse("PT3H").bind).result.head.map(
              r => assert(Duration.parse("P1DT4H1M0.335701S") === r)
            ),
            Datetimes.filter(_.id === 101L.bind).map(r => r.duration + Period.of(0, 0, 3).bind.toDuration).result.head.map(
              r => assert(Duration.parse("PT97H1M0.335701S") === r)
            ),
            // -x
            Datetimes.filter(_.id === 101L.bind).map(r => -r.duration).result.head.map(
              r => assert(Duration.parse("-P1DT1H1M0.335701S") === r)
            ),
            // -
            Datetimes.filter(_.id === 101L.bind).map(r => r.duration - Duration.parse("PT2H").bind).result.head.map(
              r => assert(Duration.parse("P1DT-1H1M0.335701S") === r)
            ),
            // *
            Datetimes.filter(_.id === 101L.bind).map(r => r.duration * 3.5).result.head.map(
              r => assert(Duration.parse("P3DT15H33M31.174954S") === r)
            ),
            // /
            Datetimes.filter(_.id === 101L.bind).map(r => r.duration / 5.0).result.head.map(
              r => assert(Duration.parse("PT5H12.06714S") === r)
            ),
            // justifyDays
            Datetimes.filter(_.id === 102L.bind).map(r => r.duration.justifyDays).result.head.map(
              r => assert(Duration.parse("P1587D") === r)
            ),
            // justifyHours
            Datetimes.filter(_.id === 103L.bind).map(r => r.duration.justifyHours).result.head.map(
              r => assert(Duration.parse("P2DT15H16M2S") === r)
            ),
            // justifyInterval
            Datetimes.filter(_.id === 103L.bind).map(r => r.duration.justifyInterval).result.head.map(
              r => assert(Duration.parse("P2DT15H16M2S") === r)
            )
          ),
          // timestamp with time zone
          DBIO.seq(
            // age
//            Datetimes.filter(_.id === 101L.bind).map(r => r.dateTimetz.age === r.dateTimetz.age(Functions.currentDate.asColumnOf[ZonedDateTime])).result.head.map(
//              r => assert(true === r)
//            ),
            // part
            Datetimes.filter(_.id === 101L.bind).map(r => r.datetimeTz.part("year")).result.head.map(
              r => assert(Math.abs(2001 - r) < 0.00001d)
            ),
            // trunc
            Datetimes.filter(_.id === 101L.bind).map(r => r.datetimeTz.trunc("day")).result.head.map(
              r => assert(ZonedDateTime.parse("2001-01-03 00:00:00+08", bpTzDateTimeFormatter) === r)
            )
          )
        )
      ).andFinally(
        Datetimes.schema drop
      ).transactionally
    ), concurrent.duration.Duration.Inf)
  }

  //////////////////////////////////////////////////////////////////////

  test("Threeten date Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getDateBean = GetResult(r => DatetimeBean(
      r.nextLong(), r.nextLocalDate(), r.nextLocalTime(), r.nextLocalDateTime(), r.nextZonedDateTime(),
      r.nextDuration(), r.nextPeriod()))

    val b = new DatetimeBean(107L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01.101357"),
      LocalDateTime.parse("2001-01-03T13:21:00.223571"), ZonedDateTime.parse("2001-01-03 13:21:00.102203+08", bpTzDateTimeFormatter),
      Duration.parse("P1DT1H1M0.335701S"), Period.parse("P1Y2M3W4D"))

    Await.result(db.run(
      DBIO.seq(
        sqlu"SET TIMEZONE TO '+8';",
        sqlu"""create table DatetimebpTest(
              id int8 not null primary key,
              date date not null,
              time time not null,
              ts timestamp not null,
              tstz timestamptz not null,
              duration interval not null,
              period interval not null)
          """,
        ///
        sqlu""" insert into DatetimebpTest values(${b.id}, ${b.date}, ${b.time}, ${b.dateTime}, ${b.dateTimetz}, ${b.duration}, ${b.period}) """,
        sql""" select * from DatetimebpTest where id = ${b.id} """.as[DatetimeBean].head.map(
          r => assert(b === r)
        ),
        ///
        sqlu"drop table if exists DatetimebpTest cascade"
      ).transactionally
    ), concurrent.duration.Duration.Inf)
  }
}
