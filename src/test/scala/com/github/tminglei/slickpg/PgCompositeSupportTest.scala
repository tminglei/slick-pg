package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import slick.driver.PostgresDriver
import scala.slick.jdbc.{StaticQuery => Q}
import java.sql.Timestamp
import java.text.SimpleDateFormat

object PgCompositeSupportTest {
  case class Composite1(
    id: Long,
    txt: String,
    date: Timestamp
    )
  
  case class Composite2(
    id: Long,
    comp1: Composite1,
    confirm: Boolean
    )

  //-------------------------------------------------------------
  trait MyCompositeSupport extends utils.PgCommonJdbcTypes with array.PgArrayJavaTypes { driver: PostgresDriver =>

    trait CompositeImplicts {
      import utils.TypeConverters.Util._
      
      utils.TypeConverters.register(mkCompositeConvFromString[Composite1])
      utils.TypeConverters.register(mkCompositeConvToString[Composite1])
      utils.TypeConverters.register(mkCompositeConvFromString[Composite2])
      utils.TypeConverters.register(mkCompositeConvToString[Composite2])
      
      implicit val composite1TypeMapper = new GenericJdbcType[Composite1]("composite1",
        mkCompositeConvFromString[Composite1], mkCompositeConvToString[Composite1])
      implicit val composite2TypeMapper = new GenericJdbcType[Composite2]("composite2",
        mkCompositeConvFromString[Composite2], mkCompositeConvToString[Composite2])
      implicit val composite1ArrayTypeMapper = new ArrayListJavaType[Composite1]("composite1",
        mkArrayConvFromString[Composite1], mkArrayConvToString[Composite1])
      implicit val composite2ArrayTypeMapper = new ArrayListJavaType[Composite2]("composite2",
        mkArrayConvFromString[Composite2], mkArrayConvToString[Composite2])
    }
  }
  
  object MyPostgresDriver1 extends MyPostgresDriver with MyCompositeSupport {
    override val Implicit = new ImplicitsPlus with CompositeImplicts {}
    override val simple = new SimpleQLPlus with CompositeImplicts {}
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
  
  class TestTable(tag: Tag) extends Table[TestBean](tag, "CompositeTest") {
    def id = column[Long]("id")
    def comps = column[List[Composite2]]("comps", O.DBType("composite2[]"))
    
    def * = (id,comps) <> (TestBean.tupled, TestBean.unapply)
  }
  val CompositeTests = TableQuery[TestTable]
  
  //-------------------------------------------------------------------
  val tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def ts(str: String) = new Timestamp(tsFormat.parse(str).getTime)
  
  val rec1 = TestBean(333, List(Composite2(201, Composite1(101, "(test1'", ts("2001-1-3 13:21:00")), true)))
  val rec2 = TestBean(335, List(Composite2(202, Composite1(102, "test2\\", ts("2012-5-8 11:31:06")), false)))
  
  @Test
  def testCompositeTypes(): Unit = {
    
    db withSession { implicit session: Session =>
      CompositeTests forceInsertAll (rec1, rec2)
      
      val q1 = CompositeTests.filter(_.id === 333L.bind).map(r => r)
      assertEquals(rec1, q1.first)
      
      val q2 = CompositeTests.filter(_.id === 335L.bind).map(r => r)
      assertEquals(rec2, q2.first)
    }
  }
  
  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      (Q[Int] + "create type composite1 as (id int8, txt text, date timestamp)").first
      (Q[Int] + "create type composite2 as (id int8, comp1 composite1, confirm boolean)").first
      
      new MyPostgresDriver1.TableDDLBuilder(CompositeTests.baseTableRow).buildDDL create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      new MyPostgresDriver1.TableDDLBuilder(CompositeTests.baseTableRow).buildDDL drop
      
      (Q[Int] + "drop type composite2").first
      (Q[Int] + "drop type composite1").first
    }
  }
}