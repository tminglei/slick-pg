package com.github.tminglei.slickpg

import java.sql.{Timestamp, Time, Date}

import org.junit._
import org.junit.Assert._
import java.util.UUID
import scala.collection.mutable.Buffer
import scala.slick.driver.PostgresDriver
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import scala.util.Try

class PgArraySupportTest {
  import utils.SimpleArrayUtils._

  //-- additional definitions
  case class Institution(value: Long)
  case class MarketFinancialProduct(value: String)

  object MyPostgresDriver1 extends PostgresDriver with PgArraySupport {
    override lazy val Implicit = new Implicits with ArrayImplicits with MyArrayImplicitsPlus {}
    override val simple = new SimpleQL with ArrayImplicits with MyArrayImplicitsPlus {}

    ///
    trait MyArrayImplicitsPlus {
      implicit val simpleLongBufferTypeMapper = new SimpleArrayJdbcType[Long]("int8").to(_.toBuffer)
      implicit val simpleStrVectorTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toVector)
      implicit val institutionListTypeWrapper =  new SimpleArrayJdbcType[Institution]("int8")
        .basedOn[Long](_.value, new Institution(_)).to(_.toList)
      implicit val marketFinancialProductWrapper = new SimpleArrayJdbcType[MarketFinancialProduct]("text")
        .basedOn[String](_.value, new MarketFinancialProduct(_)).to(_.toList)
      ///
      implicit val advancedStringListTypeMapper = new AdvancedArrayJdbcType[String]("text",
        fromString(identity)(_).orNull, mkString(identity))
    }
  }

  //////////////////////////////////////////////////////////////////////////
  import MyPostgresDriver1.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=postgres", driver = "org.postgresql.Driver")

  case class ArrayBean(
    id: Long,
    intArr: List[Int],
    longArr: Buffer[Long],
    strList: List[String],
    strArr: Option[Vector[String]],
    uuidArr: List[UUID],
    institutions: List[Institution],
    mktFinancialProducts: Option[List[MarketFinancialProduct]]
    )

  class ArrayTestTable(tag: Tag) extends Table[ArrayBean](tag, "ArrayTest") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def intArr = column[List[Int]]("intArray", O.Default(Nil))
    def longArr = column[Buffer[Long]]("longArray")
    def strList = column[List[String]]("stringList")
    def strArr = column[Option[Vector[String]]]("stringArray")
    def uuidArr = column[List[UUID]]("uuidArray")
    def institutions = column[List[Institution]]("institutions")
    def mktFinancialProducts = column[Option[List[MarketFinancialProduct]]]("mktFinancialProducts")

    def * = (id, intArr, longArr, strList, strArr, uuidArr, institutions, mktFinancialProducts) <> (ArrayBean.tupled, ArrayBean.unapply)
  }
  val ArrayTests = TableQuery[ArrayTestTable]

  //------------------------------------------------------------------------------

  val uuid1 = UUID.randomUUID()
  val uuid2 = UUID.randomUUID()
  val uuid3 = UUID.randomUUID()

  val testRec1 = ArrayBean(33L, List(101, 102, 103), Buffer(1L, 3L, 5L, 7L), List("robert}; drop table students--"), Some(Vector("str1", "str3")),
    List(uuid1, uuid2), List(Institution(113)), None)
  val testRec2 = ArrayBean(37L, List(101, 103), Buffer(11L, 31L, 5L), List(""), Some(Vector("str11", "str3")),
    List(uuid1, uuid2, uuid3), List(Institution(579)), Some(List(MarketFinancialProduct("product1"))))
  val testRec3 = ArrayBean(41L, List(103, 101), Buffer(11L, 5L, 31L), Nil, Some(Vector("(s)", "str5", "str3")),
    List(uuid1, uuid3), Nil, Some(List(MarketFinancialProduct("product3"), MarketFinancialProduct("product x"))))

  @Test
  def testArrayFunctions(): Unit = {
    db withSession { implicit session: Session =>
      Try { (ArrayTests.ddl) drop }
      Try { (ArrayTests.ddl).createStatements.foreach(s => println(s"[array] $s")) }
      Try { (ArrayTests.ddl) create }

      ArrayTests forceInsertAll (testRec1, testRec2, testRec3)

      val q0 = ArrayTests.sortBy(_.id).map(r => r)
      println(s"[array] sql = ${q0.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q0.list)

      val q1 = ArrayTests.filter(101.bind === _.intArr.any).sortBy(_.id).map(r => r)
      println(s"[array] 'any' sql = ${q1.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q1.list)

      val q2 = ArrayTests.filter(5L.bind <= _.longArr.all).sortBy(_.id).map(r => r)
      println(s"[array] 'all' sql = ${q2.selectStatement}")
      assertEquals(List(testRec2, testRec3), q2.list)

      val q3 = ArrayTests.filter(_.strArr @> Vector("str3")).sortBy(_.id).map(r => r)
      println(s"[array] '@>' sql = ${q3.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q3.list)

      val q31 = ArrayTests.filter(_.strArr @> Vector("str3").bind).sortBy(_.id).map(r => r)
      println(s"[array] '@>' sql = ${q31.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q31.list)

      val q32 = ArrayTests.filter(Vector("str3").bind <@: _.strArr).sortBy(_.id).map(r => r)
      println(s"[array] '<@' sql = ${q32.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q32.list)

      val q4 = ArrayTests.filter(_.longArr @& Buffer(5L, 17L).bind).sortBy(_.id).map(r => r)
      println(s"[array] '&&' sql = ${q4.selectStatement}")
      assertEquals(List(testRec1, testRec2, testRec3), q4.list)

      val q5 = ArrayTests.filter(_.longArr.length() > 3.bind).sortBy(_.id).map(r => r)
      println(s"[array] 'length' sql = ${q5.selectStatement}")
      assertEquals(List(testRec1), q5.list)

      val q6 = ArrayTests.filter(5L.bind <= _.longArr.all).map(_.strArr.unnest)
      println(s"[array] 'unnest' sql = ${q6.selectStatement}")
      assertEquals((testRec2.strArr.get ++ testRec3.strArr.get).toList, q6.list.map(_.orNull))

      val q7 = ArrayTests.filter(_.id === 33L.bind).map(_.intArr ++ List(105, 107).bind)
      println(s"[array] concatenate1 sql = ${q7.selectStatement}")
      assertEquals(List(101, 102, 103, 105, 107), q7.first)

      val q8 = ArrayTests.filter(_.id === 33L.bind).map(List(105, 107).bind ++ _.intArr)
      println(s"[array] concatenate2 sql = ${q8.selectStatement}")
      assertEquals(List(105, 107, 101, 102, 103), q8.first)

      val q9 = ArrayTests.filter(_.id === 33L.bind).map(_.intArr + 105.bind)
      println(s"[array] concatenate3 sql = ${q9.selectStatement}")
      assertEquals(List(101, 102, 103, 105), q9.first)

      val q10 = ArrayTests.filter(_.id === 33L.bind).map(105.bind +: _.intArr)
      println(s"[array] concatenate4 sql = ${q10.selectStatement}")
      assertEquals(List(105, 101, 102, 103), q10.first)

      // test array type mapper's 'updateObject' method
      ArrayTests.filter(_.id === 33L.bind).map(r => r).mutate({ m =>
        m.row = m.row.copy( longArr = Buffer(3, 5, 9))
      })
      val q11 = ArrayTests.filter(_.id === 33L.bind).map(r => r.longArr)
      assertEquals(List(3,5,9), q11.first)
    }
  }

  //------------------------------------------------------------------------

  @Test
  def testPlainArrayFunctions(): Unit = {
    case class ArrayBean1(
      id: Long,
      uuidArr: List[UUID],
      strArr: Seq[String],
      longArr: Seq[Long],
      intArr: List[Int],
      shortArr: Vector[Short],
      floatArr: List[Float],
      doubleArr: List[Double],
      boolArr: Seq[Boolean],
      dateArr: List[Date],
      timeArr: List[Time],
      tsArr: Seq[Timestamp]
      )

    import MyPlainPostgresDriver.plainImplicits._

    implicit val getArrarBean1Result = GetResult(r =>
      ArrayBean1(r.nextLong(),
        r.nextUUIDArray().toList,
        r.nextStringArray(),
        r.nextLongArray(),
        r.nextIntArray().toList,
        r.nextShortArray().to[Vector],
        r.nextFloatArray().toList,
        r.nextDoubleArray().toList,
        r.nextBooleanArray(),
        r.nextDateArray().toList,
        r.nextTimeArray().toList,
        r.nextTimestampArray()
      )
    )

    db withSession { implicit session: Session =>
      Try { Q.updateNA("drop table if exists ArrayTest1 cascade").execute }
      Try {
        Q.updateNA("create table ArrayTest1("+
          "id int8 not null primary key, "+
          "uuid_arr uuid[] not null, "+
          "str_arr text[] not null, "+
          "long_arr int8[] not null, "+
          "int_arr int4[] not null, "+
          "short_arr int2[] not null, "+
          "float_arr float4[] not null, "+
          "double_arr float8[] not null, "+
          "bool_arr bool[] not null, "+
          "date_arr date[] not null, "+
          "time_arr time[] not null, "+
          "ts_arr timestamp[] not null)"
        ).execute
      }

      val bean1 = ArrayBean1(101L, List(UUID.randomUUID()), List("tewe", "ttt"), List(111L), List(1, 2), Vector(3, 5), List(1.2f, 43.32f), List(21.35d), List(true, true),
        List(new Date(System.currentTimeMillis())), List(new Time(System.currentTimeMillis())), List(new Timestamp(System.currentTimeMillis())))

      def insert(b: ArrayBean1) =
        (Q.u + "insert into ArrayTest1 values("
          +? b.id + ","
          +? b.uuidArr + ","
          +? b.strArr + ","
          +? b.longArr + ","
          +? b.intArr + ","
          +? b.shortArr + ","
          +? b.floatArr + ","
          +? b.doubleArr + ","
          +? b.boolArr + ","
          +? b.dateArr + ","
          +? b.timeArr + ","
          +? b.tsArr + ")"
          ).execute

      insert(bean1)

      val found = (Q[ArrayBean1] + "select * from ArrayTest1 where id=" +? bean1.id).first

      bean1.uuidArr.zip(found.uuidArr).map(r => assertEquals(r._1, r._2))
      bean1.strArr.zip(found.strArr).map(r => assertEquals(r._1, r._2))
      bean1.longArr.zip(found.longArr).map(r => assertEquals(r._1, r._2))
      bean1.intArr.zip(found.intArr).map(r => assertEquals(r._1, r._2))
      bean1.shortArr.zip(found.shortArr).map(r => assertEquals(r._1, r._2))
      bean1.floatArr.zip(found.floatArr).map(r => assertEquals(r._1, r._2, 0.01f))
      bean1.doubleArr.zip(found.doubleArr).map(r => assertEquals(r._1, r._2, 0.01d))
      bean1.boolArr.zip(found.boolArr).map(r => assertEquals(r._1, r._2))
      bean1.dateArr.zip(found.dateArr).map(r => assertEquals(r._1.toString, r._2.toString))
      bean1.timeArr.zip(found.timeArr).map(r => assertEquals(r._1.toString, r._2.toString))
      bean1.tsArr.zip(found.tsArr).map(r => assertEquals(r._1.toString, r._2.toString))
    }
  }
}
