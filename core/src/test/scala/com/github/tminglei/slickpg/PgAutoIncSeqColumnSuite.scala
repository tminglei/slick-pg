package com.github.tminglei.slickpg

import org.scalatest.funsuite.AnyFunSuite
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PgAutoIncSeqColumnSuite extends AnyFunSuite with PostgresContainer {
  import ExPostgresProfile.api._

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  case class User(id: String, name: String)

  class AutoIncSeqTestTable(tag: Tag) extends Table[User](tag, "test_user_auto_inc_seq") {
    def id = column[String]("id", O.AutoInc, O.AutoIncSeq, O.PrimaryKey)

    def name = column[String]("name")

    def * = (id, name) <> (User.tupled, User.unapply)
  }

  val AutoIncSeqTests = TableQuery[AutoIncSeqTestTable]

  class AutoIncSeqNameTestTable(tag: Tag) extends Table[User](tag, "test_user_auto_inc_seq_name") {
    def id = column[String]("id", O.AutoInc, O.AutoIncSeq, O.AutoIncSeqName("id_seq"), O.PrimaryKey)

    def name = column[String]("name")

    def * = (id, name) <> (User.tupled, User.unapply)
  }

  val AutoIncSeqNameTests = TableQuery[AutoIncSeqNameTestTable]

  class AutoIncSeqFnTestTable(tag: Tag) extends Table[User](tag, "test_user_auto_inc_seq_fn") {
    def id = column[String]("id", O.AutoInc, O.AutoIncSeq, O.AutoIncSeqFn(next => s"'Prefix' || $next"), O.PrimaryKey)

    def name = column[String]("name")

    def * = (id, name) <> (User.tupled, User.unapply)
  }

  val AutoIncSeqFnTests = TableQuery[AutoIncSeqFnTestTable]

  class AutoIncSeqNameWithFnTestTable(tag: Tag) extends Table[User](tag, "test_user_auto_inc_seq_fn") {
    def id = column[String](
      "id",
      O.AutoInc,
      O.AutoIncSeq,
      O.AutoIncSeqName("id_seq"),
      O.AutoIncSeqFn(next => s"'Prefix' || $next"),
      O.PrimaryKey
    )

    def name = column[String]("name")

    def * = (id, name) <> (User.tupled, User.unapply)
  }

  val AutoIncSeqNameWithFnTests = TableQuery[AutoIncSeqNameWithFnTestTable]

  //------------------------------------------------------------------------------

  test("AutoIncSeq support") {
    Await.result(db.run(
      DBIO.seq(
        AutoIncSeqTests.schema create,
        sql"""SELECT last_value FROM test_user_auto_inc_seq_id_seq;""".as[Long].head.map(
          r => assert(1 === r)
        ),
        AutoIncSeqTests ++= Seq(
          User("", "user1"),
          User("", "user2"),
        )
      ).andThen {
        DBIO.seq(
          sql"""SELECT last_value FROM test_user_auto_inc_seq_id_seq;""".as[Long].head.map(
            r => assert(2 === r)
          ),
          AutoIncSeqTests.sortBy(_.id).to[List].result.map(
            r => assert(
              Seq(
                User("1", "user1"),
                User("2", "user2"),
              ) === r
            )
          )
        )
      }.andFinally {
        AutoIncSeqTests.schema drop
      }.transactionally
    ), Duration.Inf)
  }

  test("AutoIncSeqName support") {
    Await.result(db.run(
      DBIO.seq(
        AutoIncSeqNameTests.schema create,
        sql"""SELECT last_value FROM id_seq;""".as[Long].head.map(
          r => assert(1 === r)
        ),
        AutoIncSeqNameTests ++= Seq(
          User("", "user1"),
          User("", "user2"),
        )
      ).andThen {
        DBIO.seq(
          sql"""SELECT last_value FROM id_seq;""".as[Long].head.map(
            r => assert(2 === r)
          ),
          AutoIncSeqNameTests.sortBy(_.id).to[List].result.map(
            r => assert(
              Seq(
                User("1", "user1"),
                User("2", "user2"),
              ) === r
            )
          )
        )
      }.andFinally {
        AutoIncSeqNameTests.schema drop
      }.transactionally
    ), Duration.Inf)
  }

  test("AutoIncSeqFn support") {
    Await.result(db.run(
      DBIO.seq(
        AutoIncSeqFnTests.schema create,
        sql"""SELECT last_value FROM test_user_auto_inc_seq_fn_id_seq;""".as[Long].head.map(
          r => assert(1 === r)
        ),
        AutoIncSeqFnTests ++= Seq(
          User("", "user1"),
          User("", "user2"),
        )
      ).andThen {
        DBIO.seq(
          sql"""SELECT last_value FROM test_user_auto_inc_seq_fn_id_seq;""".as[Long].head.map(
            r => assert(2 === r)
          ),
          AutoIncSeqFnTests.sortBy(_.id).to[List].result.map(
            r => assert(
              Seq(
                User("Prefix1", "user1"),
                User("Prefix2", "user2"),
              ) === r
            )
          )
        )
      }.andFinally {
        AutoIncSeqFnTests.schema drop
      }.transactionally
    ), Duration.Inf)
  }

  test("AutoIncSeqName with AutoIncSeqFn support") {
    Await.result(db.run(
      DBIO.seq(
        AutoIncSeqNameWithFnTests.schema create,
        sql"""SELECT last_value FROM id_seq;""".as[Long].head.map(
          r => assert(1 === r)
        ),
        AutoIncSeqNameWithFnTests ++= Seq(
          User("", "user1"),
          User("", "user2"),
        )
      ).andThen {
        DBIO.seq(
          sql"""SELECT last_value FROM id_seq;""".as[Long].head.map(
            r => assert(2 === r)
          ),
          AutoIncSeqNameWithFnTests.sortBy(_.id).to[List].result.map(
            r => assert(
              Seq(
                User("Prefix1", "user1"),
                User("Prefix2", "user2"),
              ) === r
            )
          )
        )
      }.andFinally {
        AutoIncSeqNameWithFnTests.schema drop
      }.transactionally
    ), Duration.Inf)
  }

}
