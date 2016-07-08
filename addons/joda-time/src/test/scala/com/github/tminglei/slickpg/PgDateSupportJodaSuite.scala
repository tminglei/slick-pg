package com.github.tminglei.slickpg

import java.util.concurrent.Executors

import org.joda.time._
import org.scalatest.FunSuite
import slick.jdbc.{GetResult, PostgresProfile}

import scala.concurrent.{Await, ExecutionContext}

class PgDateSupportJodaSuite extends FunSuite {
  implicit val testExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  object MyPostgresProfile extends PostgresProfile
                            with PgDateSupportJoda {

    override val api = new API with DateTimeImplicits

    ///
    val plainAPI = new API with JodaDateTimePlainImplicits
  }

  ///
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class DatetimeBean(
    id: Long,
    date: LocalDate,
    time: LocalTime,
    dateTime: LocalDateTime,
    dateTimeTz: DateTime,
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

  test("Joda time Lifted support") {
    val now = LocalDateTime.now
    val now1 = DateTime.now
    val now2 = LocalTime.now

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
          Datetimes.filter(_.id === 101L.bind).map { r => (r.date.isFinite, r.datetime.isFinite, r.interval.isFinite) }.result.head.map(
            r => assert((true, true, true) === r)
          ),
          // +
          Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time).result.head.map(
            r => assert(LocalDateTime.parse("2010-11-03T12:33:01.101357") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date).result.head.map(
            r => assert(LocalDateTime.parse("2010-11-03T12:33:01.101") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => (r.date +++ r.interval, r.date +++ Period.days(1))).result.head.map {
            case (r1, r2) => assert(LocalDateTime.parse("2010-11-04T01:01:00.335") === r1); assert(LocalDateTime.parse("2010-11-04T00:00:00.000") === r2)
          },
          Datetimes.filter(_.id === 101L.bind).map(r => (r.time +++ r.interval, r.time +++ Period.hours(1))).result.head.map {
            case (r1, r2) => assert(LocalTime.parse("13:34:01.436") === r1); assert(LocalTime.parse("13:33:01.101") === r2)
          },
          Datetimes.filter(_.id === 101L.bind).map(r => (r.datetime +++ r.interval, r.datetime +++ Period.days(1))).result.head.map {
            case (r1, r2) => assert(LocalDateTime.parse("2001-01-04T14:22:00.558") === r1); assert(LocalDateTime.parse("2001-01-04T13:21:00.223") === r2)
          },
          Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind).result.head.map(
            r => assert(LocalDate.parse("2010-11-10") === r)
          ),
          // -
          Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind).result.head.map(
            r => assert(LocalDate.parse("2010-11-02") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime -- r.time).result.head.map(
            r => assert(LocalDateTime.parse("2001-01-03T00:47:59.122") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime - r.date).result.head.map(
            r => assert(Period.parse("P-3590DT-10H-38M-60S").plus(Period.parse("PT0.222S")) === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date.asColumnOf[LocalDateTime] - r.datetime).result.head.map(
            r => assert(Period.parse("P3590DT10H38M59.777S") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.date - LocalDate.parse("2009-07-05")).result.head.map(
            r => assert(486 === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time - LocalTime.parse("02:37:00").bind).result.head.map(
            r => assert(Period.parse("PT9H56M1.100S") === r)
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => (r.datetime --- r.interval, r.datetime --- Period.days(1))).result.head.map {
            case (r1, r2) => assert(LocalDateTime.parse("2001-01-02T12:19:59.888") === r1); assert(LocalDateTime.parse("2001-01-02T13:21:00.223") === r2)
          },
          Datetimes.filter(_.id === 101L.bind).map(r => (r.time --- r.interval, r.time --- Period.hours(1))).result.head.map {
            case (r1, r2) => assert(LocalTime.parse("11:32:00.766") === r1); assert(LocalTime.parse("11:33:01.101") === r2)
          },
          Datetimes.filter(_.id === 101L.bind).map(r => (r.date --- r.interval, r.date --- Period.days(1))).result.head.map {
            case (r1, r2) => assert(LocalDateTime.parse("2010-11-01T22:58:59.665") === r1); assert(LocalDateTime.parse("2010-11-02T00:00:00.000") === r2)
          },
          // age
//          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.age === r.datetime.age(Functions.currentDate.asColumnOf[LocalDateTime])).result.head.map(
//            r => assert(true === r)
//          ),
          // part
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.part("year")).result.head.map(
            r => assert(Math.abs(2001 - r) < 0.00001d)
          ),
          Datetimes.filter(_.id === 102L.bind).map(r => r.interval.part("year")).result.head.map(
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
          Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.atTimeZone("MST")).result.head.map(
            r => assert(r.isInstanceOf[LocalDateTime])
          ),
          Datetimes.filter(_.id === 101L.bind).map(r => r.time.atTimeZone("MST")).result.head.map(
            r => assert(r.isInstanceOf[DateTime])
          ),
          // interval
          DBIO.seq(
            // +
            Datetimes.filter(_.id === 101L.bind).map(r => r.interval + Period.parse("PT3H").bind).result.head.map(
              r => assert(Period.parse("P1DT4H1M0.335S") === r)
            ),
            // -x
            Datetimes.filter(_.id === 101L.bind).map(r => -r.interval).result.head.map(
              r => assert(Period.parse("P-1DT-1H-1M-1S").plus(Period.parse("PT0.665S")) === r)
            ),
            // -
            Datetimes.filter(_.id === 101L.bind).map(r => r.interval - Period.hours(2)).result.head.map(
              r => assert(Period.parse("P1DT-58M-60S").plus(Period.parse("PT0.335S")) === r)
            ),
            // *
            Datetimes.filter(_.id === 101L.bind).map(r => r.interval * 3.5).result.head.map(
              r => assert(Period.parse("P3DT15H33M31.172S") === r)
            ),
            // /
            Datetimes.filter(_.id === 101L.bind).map(r => r.interval / 5.0).result.head.map(
              r => assert(Period.parse("PT5H12.067S") === r)
            ),
            // justifyDays
            Datetimes.filter(_.id === 102L.bind).map(r => r.interval.justifyDays).result.head.map(
              r => assert(Period.parse("P4Y4M27D") === r)
            ),
            // justifyHours
            Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyHours).result.head.map(
              r => assert(Period.parse("P2DT15H16M2S") === r)
            ),
            // justifyInterval
            Datetimes.filter(_.id === 103L.bind).map(r => r.interval.justifyInterval).result.head.map(
              r => assert(Period.parse("P2DT15H16M2S") === r)
            )
          ),
          // timestamp with time zone
          DBIO.seq(
            // age
//            Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.age === r.datetimetz.age(Functions.currentDate.asColumnOf[DateTime])).result.head.map(
//              r => assert(true === r)
//            ),
            // part
            Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.part("year")).result.head.map(
              r => assert(Math.abs(2001 - r) < 0.00001d)
            ),
            // trunc
            Datetimes.filter(_.id === 101L.bind).map(r => r.datetimetz.trunc("day")).result.head.map(
              r => assert(DateTime.parse("2001-01-03 00:00:00.000+08", jodaTzDateTimeFormatter) === r)
            )
          ),
          // update and check
          DBIO.seq(
            Datetimes.filter(_.id === 101L.bind).map(_.datetime).update(now),
            Datetimes.filter(_.id === 101L.bind).map(_.datetime).result.head.map(
              r => assert(now === r)
            ),
            //
            Datetimes.filter(_.id === 101L.bind).map(_.datetimetz).update(now1),
            Datetimes.filter(_.id === 101L.bind).map(_.datetimetz).result.head.map(
              r => assert(now1 === r)
            ),
            //
            Datetimes.filter(_.id === 101L.bind).map(_.time).update(now2),
            Datetimes.filter(_.id === 101L.bind).map(_.time).result.head.map(
              r => assert(now2 === r)
            )
          )
        )
      ).andFinally(
        Datetimes.schema drop
      ).transactionally
    ), concurrent.duration.Duration.Inf)
  }

  //////////////////////////////////////////////////////////////////////

  test("Joda time Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getDateBean = GetResult(r => DatetimeBean(
      r.nextLong(), r.nextLocalDate(), r.nextLocalTime(), r.nextLocalDateTime(), r.nextZonedDateTime(), r.nextPeriod()))

    val b1 = new DatetimeBean(107L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01.101357"),
      LocalDateTime.parse("2001-01-03T13:21:00.223571"), DateTime.parse("2001-01-03 13:21:00.102203+08", jodaTzDateTimeFormatter),
      Period.parse("P1DT1H1M0.335701S"))
    val b2 = new DatetimeBean(108L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01"),
      LocalDateTime.parse("2001-01-03T13:21:00"), DateTime.parse("2001-01-03 13:21:00+08", jodaTzDateTimeFormatter_NoFraction),
      Period.parse("P1DT1H1M0.335701S"))

    Await.result(db.run(
      DBIO.seq(
        sqlu"SET TIMEZONE TO '+8';",
        sqlu"""create table DatetimeJodaTest(
              id int8 not null primary key,
              date date not null,
              time time not null,
              ts timestamp not null,
              tstz timestamptz not null,
              period interval not null)
          """,
        ///
        sqlu""" insert into DatetimeJodaTest values(${b1.id}, ${b1.date}, ${b1.time}, ${b1.dateTime}, ${b1.dateTimeTz}, ${b1.interval}) """,
        sql""" select * from DatetimeJodaTest where id = ${b1.id} """.as[DatetimeBean].head.map(
          r => assert(b1 === r)
        ),
        sqlu""" insert into DatetimeJodaTest values(${b2.id}, ${b2.date}, ${b2.time}, ${b2.dateTime}, ${b2.dateTimeTz}, ${b2.interval}) """,
        sql""" select * from DatetimeJodaTest where id = ${b2.id} """.as[DatetimeBean].head.map(
          r => assert(b2 === r)
        ),
        ///
        sqlu"drop table if exists DatetimeJodaTest cascade"
      ).transactionally
    ), concurrent.duration.Duration.Inf)
  }
}
