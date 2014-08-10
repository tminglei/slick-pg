package com.github.tminglei.slickpg
package json

import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.lifted.{ExtensionMethods, Column}
import scala.slick.driver.{JdbcTypesComponent, PostgresDriver}
import scala.slick.jdbc.JdbcType

trait PgJsonExtensions extends JdbcTypesComponent { driver: PostgresDriver =>
  import driver.Implicit._

  object JsonLibrary {
    val Arrow  = new SqlOperator("->")
    val BiArrow = new SqlOperator("->>")
    val PoundArrow  = new SqlOperator("#>")
    val PoundBiArrow = new SqlOperator("#>>")

    val arrayLength = new SqlFunction("json_array_length")
    val arrayElements = new SqlFunction("json_array_elements")
    val objectKeys = new SqlFunction("json_object_keys")
//    val rowToJson = new SqlFunction("row_to_json")  //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonEach = new SqlFunction("json_each")   //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonEachText = new SqlFunction("json_each_text")  //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonPopulateRecord = new SqlFunction("json_populate_record")  //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonPopulateRecordset = new SqlFunction("json_populate_recordset")  //not support, since "row" type not supported by slick/slick-pg yet
  }

  class JsonColumnExtensionMethods[JSONType, P1](val c: Column[P1])(
                implicit tm: JdbcType[JSONType], tm1: JdbcType[List[String]]) extends ExtensionMethods[JSONType, P1] {

    /** Note: json array's index starts with 0   */
    def ~> [P2, R](index: Column[P2])(implicit om: o#arg[Int, P2]#to[JSONType, R]) = {
        om.column(JsonLibrary.Arrow, n, index.toNode)
      }
    def ~>>[P2, R](index: Column[P2])(implicit om: o#arg[Int, P2]#to[String, R]) = {
        om.column(JsonLibrary.BiArrow, n, index.toNode)
      }
    def +> [P2, R](key: Column[P2])(implicit om: o#arg[String, P2]#to[JSONType, R]) = {
        om.column(JsonLibrary.Arrow, n, key.toNode)
      }
    def +>>[P2, R](key: Column[P2])(implicit om: o#arg[String, P2]#to[String, R]) = {
        om.column(JsonLibrary.BiArrow, n, key.toNode)
      }
    def #> [P2, R](keyPath: Column[P2])(implicit om: o#arg[List[String], P2]#to[JSONType, R]) = {
        om.column(JsonLibrary.PoundArrow, n, keyPath.toNode)
      }
    def #>>[P2, R](keyPath: Column[P2])(implicit om: o#arg[List[String], P2]#to[String, R]) = {
        om.column(JsonLibrary.PoundBiArrow, n, keyPath.toNode)
      }

    def arrayLength[R](implicit om: o#to[Int, R]) = om.column(JsonLibrary.arrayLength, n)
    def arrayElements[R](implicit om: o#to[JSONType, R]) = om.column(JsonLibrary.arrayElements, n)
    def objectKeys[R](implicit om: o#to[String, R]) = om.column(JsonLibrary.objectKeys, n)
  }
}
