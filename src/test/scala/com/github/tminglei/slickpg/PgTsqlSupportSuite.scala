package com.github.tminglei.slickpg

import java.util.UUID

import org.scalatest.FunSuite
import slick.backend.StaticDatabaseConfig

/*
 * NOTE: to check it, we need move `MyPostgresDriver.scala` from test folder to main folder
 */
//@StaticDatabaseConfig("file:src/test/resources/application.conf#tsql")
//class PgTsqlSupportSuite extends FunSuite {
//
//  test("tsql support - simple") {
//    import MyPostgresProfile.plainAPI._
//
//    /*
//     CREATE TABLE tsql_test_simple
//      (
//        id bigint NOT NULL,
//        json jsonb,
//        path ltree,
//        CONSTRAINT tsql_test_simple_pkey PRIMARY KEY (id)
//      )
//     */
//    // Will SUCCEED
//    val sql: DBIO[Seq[(Long, JsonString, LTree)]] = tsql"select * from tsql_test_simple"
//
//    /*
//     CREATE TABLE tsql_test
//      (
//        id bigint NOT NULL,
//        uuid_arr uuid[] NOT NULL,
//        map hstore NOT NULL,
//        json jsonb NOT NULL,
//        path ltree NOT NULL,
//        float_range numrange NOT NULL,
//        long_arr bigint[],
//        CONSTRAINT tsql_test_pkey PRIMARY KEY (id)
//      )
//     */
//    // Will FAIL, because of: could not find implicit value for parameter e: slick.jdbc.GetResult[(Long, Seq[A], scala.collection.immutable.Map[A,B], com.github.tminglei.slickpg.JsonString, com.github.tminglei.slickpg.LTree, com.github.tminglei.slickpg.Range[T], Seq[A])]
//    // NOTE: `pg type map to scala type with type parameter` was not supported by `slick`
////    val sql2: DBIO[Seq[(Long, Seq[UUID], Map[String, String], JsonString, LTree, Range[Float], Seq[Long])]] =
////      tsql"select * from tsql_test"
//  }
//}
