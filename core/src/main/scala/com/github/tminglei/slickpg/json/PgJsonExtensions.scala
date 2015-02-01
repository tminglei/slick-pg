package com.github.tminglei.slickpg
package json

import scala.slick.ast.{LiteralNode, Library}
import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.lifted.{FunctionSymbolExtensionMethods, OptionMapperDSL, ExtensionMethods, Column}
import scala.slick.driver.{JdbcTypesComponent, PostgresDriver}
import scala.slick.jdbc.JdbcType

trait PgJsonExtensions extends JdbcTypesComponent { driver: PostgresDriver =>
  import driver.Implicit._
  import FunctionSymbolExtensionMethods._

  trait BaseJsonAssistants[JSONType] {
    def toJson[P, R](e: Column[P])(implicit tm: JdbcType[JSONType], tm1: JdbcType[P],
          om: OptionMapperDSL.arg[Any, P]#to[JSONType, R]) =
      tm.sqlTypeName.toLowerCase match {
        case "json"  => om.column(JsonLibrary(tm.sqlTypeName).toJson, e.toNode)
        case "jsonb" => om.column(Library.Cast, JsonLibrary(tm.sqlTypeName).toJson.column[JSONType](e.toNode).toNode, LiteralNode(tm.sqlTypeName))
        case _  => new IllegalArgumentException("unsupported json type: " + tm.sqlTypeName)
      }
    def arrayToJson[P, R](e: Column[P], prettyBool: Option[Boolean] = None)(implicit tm: JdbcType[JSONType],
          tm1: JdbcType[P], om: OptionMapperDSL.arg[List[Any], P]#to[JSONType, R]) =
      tm.sqlTypeName.toLowerCase match {
        case "json"  => prettyBool match {
          case Some(bool) => om.column(JsonLibrary(tm.sqlTypeName).arrayToJson, e.toNode, LiteralNode(bool))
          case None => om.column(JsonLibrary(tm.sqlTypeName).arrayToJson, e.toNode)
        }
        case "jsonb" => prettyBool match {
          case Some(bool) => om.column(Library.Cast, JsonLibrary(tm.sqlTypeName).arrayToJson.column[JSONType](e.toNode, LiteralNode(bool)).toNode, LiteralNode(tm.sqlTypeName))
          case None => om.column(Library.Cast, JsonLibrary(tm.sqlTypeName).arrayToJson.column[JSONType](e.toNode).toNode, LiteralNode(tm.sqlTypeName))
        }
        case _  => new IllegalArgumentException("unsupported json type: " + tm.sqlTypeName)
      }
    def jsonObject[P, R](pairs: Column[P])(implicit tm: JdbcType[JSONType], tm1: JdbcType[P],
          om: OptionMapperDSL.arg[List[String], P]#to[JSONType, R]) =
      tm.sqlTypeName.toLowerCase match {
        case "json"  => om.column(JsonLibrary(tm.sqlTypeName).jsonObject, pairs.toNode)
        case "jsonb" => om.column(Library.Cast, JsonLibrary(tm.sqlTypeName).jsonObject.column[JSONType](pairs.toNode).toNode, LiteralNode(tm.sqlTypeName))
        case _  => new IllegalArgumentException("unsupported json type: " + tm.sqlTypeName)
      }
    def jsonObject[P1, P2, R](keys: Column[P1], vals: Column[P2])(implicit tm: JdbcType[JSONType], tm1: JdbcType[P1],
          tm2: JdbcType[P2], om: OptionMapperDSL.arg[List[String], P1]#arg[List[String], P2]#to[JSONType, R]) =
      tm.sqlTypeName.toLowerCase match {
        case "json"  => om.column(JsonLibrary(tm.sqlTypeName).jsonObject, keys.toNode, vals.toNode)
        case "jsonb" => om.column(Library.Cast, JsonLibrary(tm.sqlTypeName).jsonObject.column[JSONType](keys.toNode, vals.toNode).toNode, LiteralNode(tm.sqlTypeName))
        case _  => new IllegalArgumentException("unsupported json type: " + tm.sqlTypeName)
      }
  }

  //////////////////////////////////////////////////////////////////////

  case class JsonLibrary(pgjson: String) {
    val Arrow  = new SqlOperator("->")
    val BiArrow = new SqlOperator("->>")
    val PoundArrow  = new SqlOperator("#>")
    val PoundBiArrow = new SqlOperator("#>>")
    val Contains = new SqlOperator("@>")
    val ContainsBy = new SqlOperator("<@")
//    val Exists = new SqlOperator("?")
//    val ExistsAny = new SqlOperator("?|")
//    val ExistsAll = new SqlOperator("?&")

    val toJson = new SqlFunction("to_json")
    val arrayToJson = new SqlFunction("array_to_json")
//    val rowToJson = new SqlFunction("row_to_json")  //not support, since "row" type not supported by slick/slick-pg yet
    val jsonObject = new SqlFunction("json_object")

    val typeof = new SqlFunction(pgjson + "_typeof")
    val objectKeys = new SqlFunction(pgjson + "_object_keys")
    val arrayLength = new SqlFunction(pgjson + "_array_length")
    val arrayElements = new SqlFunction(pgjson + "_array_elements")
    val arrayElementsText = new SqlFunction(pgjson + "_array_elements_text")
//    val jsonEach = new SqlFunction(pgjson + "_each")   //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonEachText = new SqlFunction(pgjson + "_each_text")  //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonPopulateRecord = new SqlFunction(pgjson + "_populate_record")  //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonPopulateRecordset = new SqlFunction(pgjson + "_populate_recordset")  //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonToRecord = new SqlFunction(pgjson + "_to_record")  //not support, since "row" type not supported by slick/slick-pg yet
//    val jsonToRecordSet = new SqlFunction(pgjson + "_to_recordset")  //not support, since "row" type not supported by slick/slick-pg yet
  }

  class JsonColumnExtensionMethods[JSONType, P1](val c: Column[P1])(
                implicit tm: JdbcType[JSONType], tm1: JdbcType[List[String]]) extends ExtensionMethods[JSONType, P1] {
    val jsonLib = JsonLibrary(tm.sqlTypeName)
    /** Note: json array's index starts with 0   */
    def ~> [P2, R](index: Column[P2])(implicit om: o#arg[Int, P2]#to[JSONType, R]) = {
        om.column(jsonLib.Arrow, n, index.toNode)
      }
    def ~>>[P2, R](index: Column[P2])(implicit om: o#arg[Int, P2]#to[String, R]) = {
        om.column(jsonLib.BiArrow, n, index.toNode)
      }
    def +> [P2, R](key: Column[P2])(implicit om: o#arg[String, P2]#to[JSONType, R]) = {
        om.column(jsonLib.Arrow, n, key.toNode)
      }
    def +>>[P2, R](key: Column[P2])(implicit om: o#arg[String, P2]#to[String, R]) = {
        om.column(jsonLib.BiArrow, n, key.toNode)
      }
    def #> [P2, R](keyPath: Column[P2])(implicit om: o#arg[List[String], P2]#to[JSONType, R]) = {
        om.column(jsonLib.PoundArrow, n, keyPath.toNode)
      }
    def #>>[P2, R](keyPath: Column[P2])(implicit om: o#arg[List[String], P2]#to[String, R]) = {
        om.column(jsonLib.PoundBiArrow, n, keyPath.toNode)
      }
    def @>[P2,R](c2: Column[P2])(implicit om: o#arg[JSONType, P2]#to[Boolean, R]) = {
        om.column(jsonLib.Contains, n, c2.toNode)
      }
    def <@:[P2,R](c2: Column[P2])(implicit om: o#arg[JSONType, P2]#to[Boolean, R]) = {
        om.column(jsonLib.ContainsBy, c2.toNode, n)
      }

    def tpe[R](implicit om: o#to[String, R]) = om.column(jsonLib.typeof, n)
    def objectKeys[R](implicit om: o#to[String, R]) = om.column(jsonLib.objectKeys, n)
    def arrayLength[R](implicit om: o#to[Int, R]) = om.column(jsonLib.arrayLength, n)
    def arrayElements[R](implicit om: o#to[JSONType, R]) = om.column(jsonLib.arrayElements, n)
    def arrayElementsText[R](implicit om: o#to[String, R]) = om.column(jsonLib.arrayElementsText, n)
  }
}
