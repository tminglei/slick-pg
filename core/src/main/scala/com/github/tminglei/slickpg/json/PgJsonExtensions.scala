package com.github.tminglei.slickpg
package json

import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.lifted.{ExtensionMethods, TypeMapper, Column}
import scala.slick.ast.Node

trait PgJsonExtensions {
  
  type JSONType

  object JsonLibrary {
    val ->  = new SqlOperator("->")
    val ->> = new SqlOperator("->>")
    val #>  = new SqlOperator("#>")
    val #>> = new SqlOperator("#>>")

    val arrayLength = new SqlFunction("json_array_length")
    val arrayElements = new SqlFunction("json_array_elements")
    val objectKeys = new SqlFunction("json_object_keys")
//    val rowToJson = new SqlFunction("row_to_json")  //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonEach = new SqlFunction("json_each")   //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonEachText = new SqlFunction("json_each_text")  //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonPopulateRecord = new SqlFunction("json_populate_record")  //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonPopulateRecordset = new SqlFunction("json_populate_recordset")  //not support, since "row" type not supported by slick/slick-pg yet
  }

  class JsonColumnExtensionMethods[P1](val c: Column[P1])(
                implicit tm: TypeMapper[JSONType], tm1: TypeMapper[List[String]]) extends ExtensionMethods[JSONType, P1] {

    /** Note: json array's index starts with 0   */
    def ~> [P2, R](index: Column[P2])(implicit om: o#arg[Int, P2]#to[JSONType, R]) = {
        om(JsonLibrary.->.column[JSONType](n, Node(index)))
      }
    def ~>>[P2, R](index: Column[P2])(implicit om: o#arg[Int, P2]#to[String, R]) = {
        om(JsonLibrary.->>.column[String](n, Node(index)))
      }
    def +> [P2, R](key: Column[P2])(implicit om: o#arg[String, P2]#to[JSONType, R]) = {
        om(JsonLibrary.->.column[JSONType](n, Node(key)))
      }
    def +>>[P2, R](key: Column[P2])(implicit om: o#arg[String, P2]#to[String, R]) = {
        om(JsonLibrary.->>.column[String](n, Node(key)))
      }
    def #> [P2, R](keyPath: Column[P2])(implicit om: o#arg[List[String], P2]#to[JSONType, R]) = {
        om(JsonLibrary.#>.column[JSONType](n, Node(keyPath)))
      }
    def #>>[P2, R](keyPath: Column[P2])(implicit om: o#arg[List[String], P2]#to[String, R]) = {
        om(JsonLibrary.#>>.column[String](n, Node(keyPath)))
      }

    def arrayLength[R](implicit om: o#to[Int, R]) = om(JsonLibrary.arrayLength.column(n))
    def arrayElements[R](implicit om: o#to[JSONType, R]) = om(JsonLibrary.arrayElements.column(n))
    def objectKeys[R](implicit om: o#to[String, R]) = om(JsonLibrary.objectKeys.column(n))
  }
}
