package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import org.threeten.bp.{Duration, LocalDateTime, LocalTime, LocalDate}

class PgDateTime2SupportTest {
  import MyPostgresDriver2.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class DatetimeBean(
    id: Long,
    date: LocalDate,
    time: LocalTime,
    dateTime: LocalDateTime,
    duration: Duration
    )

  class DatetimeTable(tag: Tag) extends Table[DatetimeBean](tag,"Datetime2Test") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def date = column[LocalDate]("date")
    def time = column[LocalTime]("time")
    def datetime = column[LocalDateTime]("datetime")
    def duration = column[Duration]("duration")

    def * = (id, date, time, datetime, duration) <> (DatetimeBean.tupled, DatetimeBean.unapply)
  }
  val Datetimes = TableQuery[DatetimeTable]

  //------------------------------------------------------------------------------

  val testRec1 = new DatetimeBean(101L, LocalDate.parse("2010-11-03"), LocalTime.parse("12:33:01"),
    LocalDateTime.parse("2001-01-03T13:21:00"), Duration.parse("P1DT1H"))
  val testRec2 = new DatetimeBean(102L, LocalDate.parse("2011-03-02"), LocalTime.parse("03:14:07"),
    LocalDateTime.parse("2012-05-08T11:31:06"), Duration.parse("P1587D"))
  val testRec3 = new DatetimeBean(103L, LocalDate.parse("2000-05-19"), LocalTime.parse("11:13:34"),
    LocalDateTime.parse("2019-11-03T13:19:03"), Duration.parse("PT63H16M2S"))

  @Test
  def testDatetimeFunctions(): Unit = {
    db withSession { implicit session: Session =>
      Datetimes ++= Seq(testRec1, testRec2, testRec3)

      // datetime - '+'/'-'
      val q1 = Datetimes.filter(_.id === 101L.bind).map(r => r.date + r.time)
      println(s"'+' sql = ${q1.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-03T12:33:01"), q1.first())

      val q101 = Datetimes.filter(_.id === 101L.bind).map(r => r.time + r.date)
      println(s"'+' sql = ${q101.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-03T12:33:01"), q101.first())

      val q2 = Datetimes.filter(_.id === 101L.bind).map(r => r.date +++ r.duration)
      println(s"'+++' sql = ${q2.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-04T01:00:00"), q2.first())

      val q3 = Datetimes.filter(_.id === 101L.bind).map(r => r.time +++ r.duration)
      println(s"'+++' sql = ${q3.selectStatement}")
      assertEquals(LocalTime.parse("13:33:01"), q3.first())

      val q4 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime +++ r.duration)
      println(s"'+++' sql = ${q4.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-04T14:21:00"), q4.first())

      val q5 = Datetimes.filter(_.id === 101L.bind).map(r => r.date ++ 7.bind)
      println(s"'++' sql = ${q5.selectStatement}")
      assertEquals(LocalDate.parse("2010-11-10"), q5.first())

      val q6 = Datetimes.filter(_.id === 101L.bind).map(r => r.date -- 1.bind)
      println(s"'--' sql = ${q6.selectStatement}")
      assertEquals(LocalDate.parse("2010-11-02"), q6.first())

      val q7 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime -- r.time)
      println(s"'--' sql = ${q7.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-03T00:47:59"), q7.first())

      val q8 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime - r.date)
      println(s"'-' sql = ${q8.selectStatement}")
      assertEquals(Duration.parse("-P3589DT10H39M"), q8.first())

      val q801 = Datetimes.filter(_.id === 101L.bind).map(r => r.date.asColumnOf[LocalDateTime] - r.datetime)
      println(s"'-' sql = ${q801.selectStatement}")
      assertEquals(Duration.parse("P3589DT10H39M"), q801.first())

      val q9 = Datetimes.filter(_.id === 101L.bind).map(r => r.date - LocalDate.parse("2009-07-05"))
      println(s"'-' sql = ${q9.selectStatement}")
      assertEquals(485, q9.first())

      val q10 = Datetimes.filter(_.id === 101L.bind).map(r => r.time - LocalTime.parse("02:37:00").bind)
      println(s"'-' sql = ${q10.selectStatement}")
      assertEquals(Duration.parse("PT9H56M1S"), q10.first())

      val q11 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime --- r.duration)
      println(s"'---' sql = ${q11.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-02T12:21:00"), q11.first())

      val q12 = Datetimes.filter(_.id === 101L.bind).map(r => r.time --- r.duration)
      println(s"'---' sql = ${q12.selectStatement}")
      assertEquals(LocalTime.parse("11:33:01"), q12.first())

      val q13 = Datetimes.filter(_.id === 101L.bind).map(r => r.date --- r.duration)
      println(s"'---' sql = ${q13.selectStatement}")
      assertEquals(LocalDateTime.parse("2010-11-01T23:00:00"), q13.first())

      // datetime - age/part/trunc
      val q14 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.age)
      val q141 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.age(Functions.currentDate.asColumnOf[LocalDateTime]))
      println(s"'age' sql = ${q14.selectStatement}")
      println(s"'age' sql1 = ${q141.selectStatement}")
      assertEquals(q141.first(), q14.first())

      val q15 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.part("year"))
      println(s"'part' sql = ${q15.selectStatement}")
      assertEquals(2001, q15.first(), 0.00001d)

      val q1501 = Datetimes.filter(_.id === 102L.bind).map(r => r.duration.part("year"))
      println(s"'part' sql = ${q1501.selectStatement}")
      assertEquals(0, q1501.first(), 0.00001d)

      val q16 = Datetimes.filter(_.id === 101L.bind).map(r => r.datetime.trunc("day"))
      println(s"'trunc' sql = ${q16.selectStatement}")
      assertEquals(LocalDateTime.parse("2001-01-03T00:00:00"), q16.first())

      // interval test cases
      val q21 = Datetimes.filter(_.id === 101L.bind).map(r => r.duration + Duration.parse("PT3H").bind)
      println(s"'+' sql = ${q21.selectStatement}")
      assertEquals(Duration.parse("P1DT4H"), q21.first())

      val q22 = Datetimes.filter(_.id === 101L.bind).map(r => -r.duration)
      println(s"'unary_-' sql = ${q22.selectStatement}")
      assertEquals(Duration.parse("-P1DT1H"), q22.first())

      val q23 = Datetimes.filter(_.id === 101L.bind).map(r => r.duration - Duration.parse("PT2H").bind)
      println(s"'-' sql = ${q23.selectStatement}")
      assertEquals(Duration.parse("P1DT-1H"), q23.first())

      val q24 = Datetimes.filter(_.id === 101L.bind).map(r => r.duration * 3.5)
      println(s"'*' sql = ${q24.selectStatement}")
      assertEquals(Duration.parse("P3DT15H30M"), q24.first())

      val q25 = Datetimes.filter(_.id === 101L.bind).map(r => r.duration / 5.0)
      println(s"'*' sql = ${q25.selectStatement}")
      assertEquals(Duration.parse("PT5H"), q25.first())

      val q26 = Datetimes.filter(_.id === 102L.bind).map(r => r.duration.justifyDays)
      println(s"'justifyDays' sql = ${q26.selectStatement}")
      assertEquals(Duration.parse("P1587D"), q26.first())

      val q27 = Datetimes.filter(_.id === 103L.bind).map(r => r.duration.justifyHours)
      println(s"'justifyHours' sql = ${q27.selectStatement}")
      assertEquals(Duration.parse("P2DT15H16M2S"), q27.first())

      val q28 = Datetimes.filter(_.id === 103L.bind).map(r => r.duration.justifyInterval)
      println(s"'justifyInterval' sql = ${q28.selectStatement}")
      assertEquals(Duration.parse("P2DT15H16M2S"), q28.first())
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      Datetimes.ddl create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      Datetimes.ddl drop
    }
  }
}
