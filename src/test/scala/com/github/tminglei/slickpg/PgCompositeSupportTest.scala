package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.{StaticQuery => Q}
import java.sql.Timestamp
import java.text.SimpleDateFormat

object PgCompositeSupportTest {
  val tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def ts(str: String) = new Timestamp(tsFormat.parse(str).getTime)

  case class Composite1(
    id: Long,
    txt: String,
    date: Timestamp,
    tsRange: Option[Range[Timestamp]]
    )
  
  case class Composite2(
    id: Long,
    comp1: Composite1,
    confirm: Boolean
    )

  case class Composite3(
    name: String,
    code: Int,
    num: Int
    )

  //-------------------------------------------------------------
  object MyPostgresDriver1 extends PostgresDriver with PgArraySupport with utils.PgCommonJdbcTypes {
    override lazy val Implicit = new Implicits with ArrayImplicits with CompositeImplicts {}
    override val simple = new SimpleQL with ArrayImplicits with CompositeImplicts {}

    ///
    trait CompositeImplicts {
      import utils.TypeConverters.Util._

      utils.TypeConverters.register(PgRangeSupportUtils.mkRangeFn(ts))
      utils.TypeConverters.register(PgRangeSupportUtils.toStringFn[Timestamp](tsFormat.format))

      utils.TypeConverters.register(mkCompositeConvFromString[Composite1])
      utils.TypeConverters.register(mkCompositeConvToString[Composite1])
      utils.TypeConverters.register(mkCompositeConvFromString[Composite2])
      utils.TypeConverters.register(mkCompositeConvToString[Composite2])
      utils.TypeConverters.register(mkCompositeConvFromString[Composite3])
      utils.TypeConverters.register(mkCompositeConvToString[Composite3])

      implicit val composite1TypeMapper = new GenericJdbcType[Composite1]("composite1",
        mkCompositeConvFromString[Composite1], mkCompositeConvToString[Composite1])
      implicit val composite2TypeMapper = new GenericJdbcType[Composite2]("composite2",
        mkCompositeConvFromString[Composite2], mkCompositeConvToString[Composite2])
      implicit val composite3TypeMapper = new GenericJdbcType[Composite3]("composite3",
        mkCompositeConvFromString[Composite3], mkCompositeConvToString[Composite3])
      implicit val composite1ArrayTypeMapper = new ArrayListJdbcType[Composite1]("composite1",
        mkArrayConvFromString[Composite1], mkArrayConvToString[Composite1])
      implicit val composite2ArrayTypeMapper = new ArrayListJdbcType[Composite2]("composite2",
        mkArrayConvFromString[Composite2], mkArrayConvToString[Composite2])
      implicit val composite3ArrayTypeMapper = new ArrayListJdbcType[Composite3]("composite3",
        mkArrayConvFromString[Composite3], mkArrayConvToString[Composite3])
    }
  }
}

///
class PgCompositeSupportTest {
  import PgCompositeSupportTest._
  import MyPostgresDriver1.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class TestBean(
    id: Long,
    comps: List[Composite2]
    )
  
  case class TestBean1(
    id: Long,
    comps: List[Composite3]
    )
  
  class TestTable(tag: Tag) extends Table[TestBean](tag, "CompositeTest") {
    def id = column[Long]("id")
    def comps = column[List[Composite2]]("comps", O.DBType("composite2[]"))
    
    def * = (id,comps) <> (TestBean.tupled, TestBean.unapply)
  }
  val CompositeTests = TableQuery(new TestTable(_))
  
  class TestTable1(tag: Tag) extends Table[TestBean1](tag, "CompositeTest1") {
    def id = column[Long]("id")
    def comps = column[List[Composite3]]("comps", O.DBType("composite3[]"))
    
    def * = (id,comps) <> (TestBean1.tupled, TestBean1.unapply)
  }
  val CompositeTests1 = TableQuery(new TestTable1(_))
  
  //-------------------------------------------------------------------
  
  val rec1 = TestBean(333, List(Composite2(201, Composite1(101, "(test1'", ts("2001-1-3 13:21:00"),
		  							Some(Range(ts("2010-01-01 14:30:00"), ts("2010-01-03 15:30:00")))), true)))
  val rec2 = TestBean(335, List(Composite2(202, Composite1(102, "test2\\", ts("2012-5-8 11:31:06"),
		  							Some(Range(ts("2011-01-01 14:30:00"), ts("2011-11-01 15:30:00")))), false)))
  val rec3 = TestBean(337, List(Composite2(203, Composite1(103, "ABC ABC", ts("2015-3-8 17:17:03"), None), false)))
  
  @Test
  def testCompositeTypes(): Unit = {
    
    db withSession { implicit session: Session =>
      CompositeTests forceInsertAll (rec1, rec2, rec3)
      
      val q1 = CompositeTests.filter(_.id === 333L.bind).map(r => r)
      assertEquals(rec1, q1.first)
      
      val q2 = CompositeTests.filter(_.id === 335L.bind).map(r => r)
      assertEquals(rec2, q2.first)

      val q3 = CompositeTests.filter(_.id === 337L.bind).map(r => r)
      assertEquals(rec3, q3.first)
    }
  }
  
  ///
  val rec11 = TestBean1(111, List(Composite3("(test1'", 101, 110)))
  val rec12 = TestBean1(112, List(Composite3("test2\\", 102, 111)))
  val rec13 = TestBean1(113, List(Composite3("ABC ABC", 103, 112)))
  
  @Test
  def testCompositeTypes1(): Unit = {
    
    db withSession { implicit session: Session =>
      CompositeTests1 forceInsertAll (rec11, rec12, rec13)
      
      val q1 = CompositeTests1.filter(_.id === 111L.bind).map(r => r)
      assertEquals(rec11, q1.first)
      
      val q2 = CompositeTests1.filter(_.id === 112L.bind).map(r => r)
      assertEquals(rec12, q2.first)

      val q3 = CompositeTests1.filter(_.id === 113L.bind).map(r => r)
      assertEquals(rec13, q3.first)
    }
  }
  
  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      (Q[Int] + "create type composite1 as (id int8, txt text, date timestamp, ts_range tsrange)").execute
      (Q[Int] + "create type composite2 as (id int8, comp1 composite1, confirm boolean)").execute
      (Q[Int] + "create type composite3 as (txt text, id int4, code int4)").execute
      
      (CompositeTests.ddl ++ CompositeTests1.ddl) create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      (CompositeTests.ddl ++ CompositeTests1.ddl) drop
      
      (Q[Int] + "drop type composite3").execute
      (Q[Int] + "drop type composite2").execute
      (Q[Int] + "drop type composite1").execute
    }
  }
}