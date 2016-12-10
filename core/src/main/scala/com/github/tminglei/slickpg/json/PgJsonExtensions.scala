package com.github.tminglei.slickpg
package json

import slick.ast.TypedType
import slick.ast.Library.{SqlFunction, SqlOperator}
import slick.lifted.ExtensionMethods
import slick.jdbc.{JdbcType, JdbcTypesComponent, PostgresProfile}

trait PgJsonExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  import driver.api._

  class JsonLibrary(pgjson: String) {
    val Arrow  = new SqlOperator("->")
    val BiArrow = new SqlOperator("->>")
    val PoundArrow  = new SqlOperator("#>")
    val PoundBiArrow = new SqlOperator("#>>")
    val Contains = new SqlOperator("@>")
    val ContainsBy = new SqlOperator("<@")
    val Exists = new SqlOperator("??")
    val ExistsAny = new SqlOperator("??|")
    val ExistsAll = new SqlOperator("??&")
    val Concatenate = new SqlOperator("||")
    val Delete = new SqlOperator("-")
    val DeleteDeep = new SqlOperator("#-")

    val toJson = new SqlFunction("to_json")
    val toJsonb = new SqlFunction("to_jsonb")
    val arrayToJson = new SqlFunction("array_to_json")
//    val rowToJson = new SqlFunction("row_to_json")  //not support, since "row" type not supported by slick/slick-pg yet
    val jsonbSet = new SqlFunction("jsonb_set")
    val jsonObject = new SqlFunction(pgjson + "_object")

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

  class JsonColumnExtensionMethods[JSONType, P1](val c: Rep[P1])(
                implicit tm: JdbcType[JSONType]) extends ExtensionMethods[JSONType, P1] {

    protected implicit def b1Type: TypedType[JSONType] = implicitly[TypedType[JSONType]]

    val jsonLib = new JsonLibrary(tm.sqlTypeName(None))
    /** Note: json array's index starts with 0   */
    def ~> [P2, R](index: Rep[P2])(implicit om: o#arg[Int, P2]#to[JSONType, R]) = {
        om.column(jsonLib.Arrow, n, index.toNode)
      }
    def ~>>[P2, R](index: Rep[P2])(implicit om: o#arg[Int, P2]#to[String, R]) = {
        om.column(jsonLib.BiArrow, n, index.toNode)
      }
    def +> [P2, R](key: Rep[P2])(implicit om: o#arg[String, P2]#to[JSONType, R]) = {
        om.column(jsonLib.Arrow, n, key.toNode)
      }
    def +>>[P2, R](key: Rep[P2])(implicit om: o#arg[String, P2]#to[String, R]) = {
        om.column(jsonLib.BiArrow, n, key.toNode)
      }
    def #> [P2, R](keyPath: Rep[P2])(implicit om: o#arg[List[String], P2]#to[JSONType, R]) = {
        om.column(jsonLib.PoundArrow, n, keyPath.toNode)
      }
    def #>>[P2, R](keyPath: Rep[P2])(implicit om: o#arg[List[String], P2]#to[String, R]) = {
        om.column(jsonLib.PoundBiArrow, n, keyPath.toNode)
      }
    def @>[P2,R](c2: Rep[P2])(implicit om: o#arg[JSONType, P2]#to[Boolean, R]) = {
        om.column(jsonLib.Contains, n, c2.toNode)
      }
    def <@:[P2,R](c2: Rep[P2])(implicit om: o#arg[JSONType, P2]#to[Boolean, R]) = {
        om.column(jsonLib.ContainsBy, c2.toNode, n)
      }
    def ??[P2, R](c2: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(jsonLib.Exists, n, c2.toNode)
      }
    def ?|[P2, R](c2: Rep[P2])(implicit om: o#arg[List[String], P2]#to[Boolean, R]) = {
        om.column(jsonLib.ExistsAny, n, c2.toNode)
      }
    def ?&[P2, R](c2: Rep[P2])(implicit om: o#arg[List[String], P2]#to[Boolean, R]) = {
        om.column(jsonLib.ExistsAll, n, c2.toNode)
      }
    def ||[P2, R](c2: Rep[P2])(implicit om: o#arg[JSONType, P2]#to[JSONType, R]) = {
        om.column(jsonLib.Concatenate, n, c2.toNode)
      }
    def - [P2, R](c2: Rep[P2])(implicit om: o#arg[String, P2]#to[JSONType, R]) = {
        om.column(jsonLib.Delete, n, c2.toNode)
      }
    def #-[P2, R](c2: Rep[P2])(implicit om: o#arg[List[String], P2]#to[JSONType, R]) = {
        om.column(jsonLib.DeleteDeep, n, c2.toNode)
      }

    def jsonType[R](implicit om: o#to[String, R]) = om.column(jsonLib.typeof, n)
    def objectKeys[R](implicit om: o#to[String, R]) = om.column(jsonLib.objectKeys, n)
    def arrayLength[R](implicit om: o#to[Int, R]) = om.column(jsonLib.arrayLength, n)
    def arrayElements[R](implicit om: o#to[JSONType, R]) = om.column(jsonLib.arrayElements, n)
    def arrayElementsText[R](implicit om: o#to[String, R]) = om.column(jsonLib.arrayElementsText, n)
    def set[R](path: Rep[List[String]], value: Rep[JSONType], createMissing: Option[Boolean] = None)(
      implicit om: o#to[JSONType, R]) = createMissing match {
        case Some(b) => om.column(jsonLib.jsonbSet, n, path.toNode, value.toNode, LiteralColumn(b).toNode)
        case None    => om.column(jsonLib.jsonbSet, n, path.toNode, value.toNode)
      }
  }
}
