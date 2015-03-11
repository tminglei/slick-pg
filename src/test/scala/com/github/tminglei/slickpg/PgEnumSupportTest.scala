package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import slick.driver.PostgresDriver

import scala.concurrent.ExecutionContext.Implicits.global

class PgEnumSupportTest {
  object WeekDays extends Enumeration {
    type WeekDay = Value
    val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
  }
  object Rainbows extends Enumeration {
    type Rainbow = Value
    val red, orange, yellow, green, blue, purple = Value
  }

  import WeekDays._
  import Rainbows._

  object MyPostgresDriver1 extends PostgresDriver with PgEnumSupport {
    override val api = new API with MyEnumImplicits {}

    trait MyEnumImplicits {
      implicit val weekDayTypeMapper = createEnumJdbcType("WeekDay", WeekDays)
      implicit val weekDayListTypeMapper = createEnumListJdbcType("weekDay", WeekDays)
      implicit val rainbowTypeMapper = createEnumJdbcType("Rainbow", Rainbows, true)
      implicit val rainbowListTypeMapper = createEnumListJdbcType("Rainbow", Rainbows, true)

      implicit val weekDayColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(WeekDays)
      implicit val weekDayOptionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder(WeekDays)
      implicit val rainbowColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(Rainbows)
      implicit val rainbowOptionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder(Rainbows)
    }
  }

  ////////////////////////////////////////////////////////////////////
  import MyPostgresDriver1.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

  case class TestEnumBean(id: Long, weekday: WeekDay, rainbow: Option[Rainbow], weekdays: List[WeekDay], rainbows: List[Rainbow])

  class TestEnumTable(tag: Tag) extends Table[TestEnumBean](tag, "test_enum_table") {
    def id = column[Long]("id")
    def weekday = column[WeekDay]("weekday", O.Default(Mon))
    def rainbow = column[Option[Rainbow]]("rainbow")
    def weekdays = column[List[WeekDay]]("weekdays")
    def rainbows = column[List[Rainbow]]("rainbows")

    def * = (id, weekday, rainbow, weekdays, rainbows) <> (TestEnumBean.tupled, TestEnumBean.unapply)
  }
  val TestEnums = TableQuery(new TestEnumTable(_))

  //------------------------------------------------------------------

  val testRec1 = TestEnumBean(101L, Mon, Some(red), Nil, List(red, yellow))
  val testRec2 = TestEnumBean(102L, Wed, Some(blue), List(Sat, Sun), List(green))
  val testRec3 = TestEnumBean(103L, Fri, None, List(Thu), Nil)

  @Test
  def testEnumFunctions(): Unit = {
    db.run(DBIO.seq(
      PgEnumSupportUtils.buildCreateSql("WeekDay", WeekDays),
      PgEnumSupportUtils.buildCreateSql("Rainbow", Rainbows, true),
      TestEnums.schema create,
      ///
      TestEnums forceInsertAll List(testRec1, testRec2, testRec3),
      // 0. simple test
      TestEnums.sortBy(_.id).to[List].result.map(
        assertEquals(List(testRec1, testRec2, testRec3), _)
      ),
      // 1. 'first'
      TestEnums.filter(_.id === 101L.bind).map(t => t.weekday.first).result.head.map(
        assertEquals(Mon, _)
      ),
      // 2. 'last'
      TestEnums.filter(_.id === 101L.bind).map(t => t.rainbow.last).result.head.map(
        assertEquals(Some(purple), _)
      ),
      // 3. 'all'
      TestEnums.filter(_.id === 101L.bind).map(t => t.weekday.all).result.head.map(
        assertEquals(WeekDays.values.toList, _)
      ),
      // 4. 'range'
      TestEnums.filter(_.id === 102L.bind).map(t => t.weekday range null.asInstanceOf[WeekDay]).result.head.map(
        assertEquals(List(Wed, Thu, Fri, Sat, Sun), _)
      ),
      TestEnums.filter(_.id === 102L.bind).map(t => null.asInstanceOf[WeekDay].bind range t.weekday).result.head.map(
        assertEquals(List(Mon, Tue, Wed), _)
      ),
      ///
      TestEnums.schema drop,
      PgEnumSupportUtils.buildDropSql("weekday"),
      PgEnumSupportUtils.buildDropSql("Rainbow", true)
    ).transactionally)
  }
}
