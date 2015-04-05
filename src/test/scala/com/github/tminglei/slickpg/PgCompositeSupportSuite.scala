package com.github.tminglei.slickpg

import org.scalatest.FunSuite

import slick.driver.PostgresDriver
import slick.jdbc.{GetResult, PositionedResult}
import scala.reflect.runtime.{universe => u}
import java.sql.Timestamp
import java.text.SimpleDateFormat
import composite.Struct

import scala.concurrent.Await
import scala.concurrent.duration._

object PgCompositeSupportSuite {
  val tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  def ts(str: String) = new Timestamp(tsFormat.parse(str).getTime)

  case class Composite1(
    id: Long,
    txt: String,
    date: Timestamp,
    tsRange: Option[Range[Timestamp]]
    ) extends Struct

  case class Composite2(
    id: Long,
    comp1: Composite1,
    confirm: Boolean
    ) extends Struct

  case class Composite3(
    name: Option[String] = None,
    code: Option[Int] = None,
    num: Option[Int] = None,
    bool: Option[Boolean] = None
    ) extends Struct

  //-------------------------------------------------------------
  object MyPostgresDriver1 extends PostgresDriver with PgCompositeSupport with PgArraySupport with utils.PgCommonJdbcTypes {
    override val api = new API with ArrayImplicits with CompositeImplicits {}

    val plainImplicits = new Implicits with CompositePlainImplicits {}

    ///
    trait CompositeImplicits {
      utils.TypeConverters.register(PgRangeSupportUtils.mkRangeFn(ts))
      utils.TypeConverters.register(PgRangeSupportUtils.toStringFn[Timestamp](tsFormat.format))

      implicit val composite1TypeMapper = createCompositeJdbcType[Composite1]("composite1")
      implicit val composite2TypeMapper = createCompositeJdbcType[Composite2]("composite2")
      implicit val composite3TypeMapper = createCompositeJdbcType[Composite3]("composite3")
      implicit val composite1ArrayTypeMapper = createCompositeArrayJdbcType[Composite1]("composite1").to(_.toList)
      implicit val composite2ArrayTypeMapper = createCompositeArrayJdbcType[Composite2]("composite2").to(_.toList)
      implicit val composite3ArrayTypeMapper = createCompositeArrayJdbcType[Composite3]("composite3").to(_.toList)
    }

    trait CompositePlainImplicits extends SimpleArrayPlainImplicits {
      implicit class MyCompositePositionedResult(r: PositionedResult) {
        def nextComposite1() = nextComposite[Composite1](r)
        def nextComposite2() = nextComposite[Composite2](r)
        def nextComposite3() = nextComposite[Composite3](r)
      }
      override protected def extNextArray(tpe: u.Type, r: PositionedResult): (Boolean, Option[Seq[_]]) =
        tpe match {
          case tpe if tpe.typeConstructor =:= u.typeOf[Composite1].typeConstructor =>
            (true, nextCompositeArray[Composite1](r))
          case tpe if tpe.typeConstructor =:= u.typeOf[Composite2].typeConstructor =>
            (true, nextCompositeArray[Composite2](r))
          case tpe if tpe.typeConstructor =:= u.typeOf[Composite3].typeConstructor =>
            (true, nextCompositeArray[Composite3](r))
          case _ => super.extNextArray(tpe, r)
        }

      implicit val composite1SetParameter = createCompositeSetParameter[Composite1]("composite1")
      implicit val composite1OptSetParameter = createCompositeOptionSetParameter[Composite1]("composite1")
      implicit val composite1ArraySetParameter = createCompositeArraySetParameter[Composite1]("composite1")
      implicit val composite1ArrayOptSetParameter = createCompositeOptionArraySetParameter[Composite1]("composite1")

      implicit val composite2SetParameter = createCompositeSetParameter[Composite2]("composite2")
      implicit val composite2OptSetParameter = createCompositeOptionSetParameter[Composite2]("composite2")
      implicit val composite2ArraySetParameter = createCompositeArraySetParameter[Composite2]("composite2")
      implicit val composite2ArrayOptSetParameter = createCompositeOptionArraySetParameter[Composite2]("composite2")

      implicit val composite3SetParameter = createCompositeSetParameter[Composite3]("composite3")
      implicit val composite3OptSetParameter = createCompositeOptionSetParameter[Composite3]("composite3")
      implicit val composite3ArraySetParameter = createCompositeArraySetParameter[Composite3]("composite3")
      implicit val composite3ArrayOptSetParameter = createCompositeOptionArraySetParameter[Composite3]("composite3")
    }
  }
}

///
class PgCompositeSupportSuite extends FunSuite {
  import PgCompositeSupportSuite._
  import MyPostgresDriver1.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

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
    def comps = column[List[Composite2]]("comps", O.Default(Nil))

    def * = (id,comps) <> (TestBean.tupled, TestBean.unapply)
  }
  val CompositeTests = TableQuery(new TestTable(_))

  class TestTable1(tag: Tag) extends Table[TestBean1](tag, "CompositeTest1") {
    def id = column[Long]("id")
    def comps = column[List[Composite3]]("comps")

    def * = (id,comps) <> (TestBean1.tupled, TestBean1.unapply)
  }
  val CompositeTests1 = TableQuery(new TestTable1(_))

  //-------------------------------------------------------------------

  val rec1 = TestBean(333, List(Composite2(201, Composite1(101, "(test1'", ts("2001-1-3 13:21:00"),
		  							Some(Range(ts("2010-01-01 14:30:00"), ts("2010-01-03 15:30:00")))), true)))
  val rec2 = TestBean(335, List(Composite2(202, Composite1(102, "test2\\", ts("2012-5-8 11:31:06"),
		  							Some(Range(ts("2011-01-01 14:30:00"), ts("2011-11-01 15:30:00")))), false)))
  val rec3 = TestBean(337, List(Composite2(203, Composite1(103, "ABC ABC", ts("2015-3-8 17:17:03"), None), false)))

  val rec11 = TestBean1(111, List(Composite3(Some("(test1'"))))
  val rec12 = TestBean1(112, List(Composite3(code = Some(102))))
  val rec13 = TestBean1(113, List(Composite3()))

  test("Composite type Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        sqlu"create type composite1 as (id int8, txt text, date timestamp, ts_range tsrange)",
        sqlu"create type composite2 as (id int8, comp1 composite1, confirm boolean)",
        sqlu"create type composite3 as (txt text, id int4, code int4, bool boolean)",
        (CompositeTests.schema ++ CompositeTests1.schema) create,
        CompositeTests forceInsertAll List(rec1, rec2, rec3),
        CompositeTests1 forceInsertAll List(rec11, rec12, rec13)
      ).andThen(
        DBIO.seq(
          CompositeTests.filter(_.id === 333L.bind).result.head.map(
            r => assert(rec1 === r)
          ),
          CompositeTests.filter(_.id === 335L.bind).result.head.map(
            r => assert(rec2 === r)
          ),
          CompositeTests.filter(_.id === 337L.bind).result.head.map(
            r => assert(rec3 === r)
          )
        )
      ).andThen(
        DBIO.seq(
          CompositeTests1.filter(_.id === 111L.bind).result.head.map(
            r => assert(rec11 === r)
          ),
          CompositeTests1.filter(_.id === 112L.bind).result.head.map(
            r => assert(rec12 === r)
          ),
          CompositeTests1.filter(_.id === 113L.bind).result.head.map(
            r => assert(rec13 === r)
          )
        )
      ).andFinally(
        DBIO.seq(
          (CompositeTests.schema ++ CompositeTests1.schema) drop,
          sqlu"drop type composite3",
          sqlu"drop type composite2",
          sqlu"drop type composite1"
        )
      ).transactionally
    ), Duration.Inf)
  }

  test("Composite type Plain SQL support") {
    import MyPostgresDriver1.plainImplicits._

    implicit val getTestBeanResult = GetResult(r => TestBean(r.nextLong(), r.nextArray[Composite2]().toList))
    implicit val getTestBean1Result = GetResult(r => TestBean1(r.nextLong(), r.nextArray[Composite3]().toList))

    Await.result(db.run(DBIO.seq(
      sqlu"create type composite1 as (id int8, txt text, date timestamp, ts_range tsrange)",
      sqlu"create type composite2 as (id int8, comp1 composite1, confirm boolean)",
      sqlu"create type composite3 as (txt text, id int4, code int4, bool boolean)",
      sqlu"create table CompositeTest (id BIGINT NOT NULL, comps composite2[] DEFAULT '{}' NOT NULL)",
      sqlu"create table CompositeTest1 (id BIGINT NOT NULL, comps composite3[] NOT NULL)",
      ///
      sqlu"insert into CompositeTest values(${rec1.id}, ${rec1.comps})",
      sql"select * from CompositeTest where id = ${rec1.id}".as[TestBean].head.map(
        r => assert(rec1 === r)
      ),
      sqlu"insert into CompositeTest1 values(${rec11.id}, ${rec11.comps})",
      sql"select * from CompositeTest1 where id = ${rec11.id}".as[TestBean1].head.map(
        r => assert(rec11 === r)
      ),
      ///
      sqlu"drop table CompositeTest1",
      sqlu"drop table CompositeTest",
      sqlu"drop type composite3",
      sqlu"drop type composite2",
      sqlu"drop type composite1"
    ).transactionally), Duration.Inf)
  }
}