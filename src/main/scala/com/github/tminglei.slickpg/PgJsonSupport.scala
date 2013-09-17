package com.github.tminglei.slickpg

import scala.slick.driver.{BasicProfile, PostgresDriver}
import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.lifted._
import scala.slick.session.{PositionedResult, PositionedParameters}
import org.postgresql.util.PGobject
import scala.slick.ast.Node
import org.json4s._

trait PgJsonSupport[T] { driver: PostgresDriver =>

  val jsonMethods: JsonMethods[T]

  trait JsonImplicits {
    implicit val jsonTypeMapper = new JsonTypeMapper(jsonMethods)

    implicit def jsonColumnExtensionMethods(c: Column[JValue])(
      implicit tm: TypeMapper[JValue], tm1: TypeMapper[List[String]]) = {
        new JsonColumnExtensionMethods[JValue](c)
      }
    implicit def jsonOptionColumnExtensionMethods(c: Column[Option[JValue]])(
      implicit tm: TypeMapper[JValue], tm1: TypeMapper[List[String]]) = {
        new JsonColumnExtensionMethods[Option[JValue]](c)
      }
  }

  /////////////////////////////////////////////////////////////////

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
            implicit tm: TypeMapper[JValue], tm1: TypeMapper[List[String]])
                  extends ExtensionMethods[JValue, P1] {

    def ~> [P2, R](index: Column[P2])(implicit om: o#arg[Int, P2]#to[JValue, R]) = {
        om(JsonLibrary.->.column[JValue](n, Node(index)))
      }
    def ~>>[P2, R](index: Column[P2])(implicit om: o#arg[Int, P2]#to[String, R]) = {
        om(JsonLibrary.->>.column[String](n, Node(index)))
      }
    def +> [P2, R](key: Column[P2])(implicit om: o#arg[String, P2]#to[JValue, R]) = {
        om(JsonLibrary.->.column[JValue](n, Node(key)))
      }
    def +>>[P2, R](key: Column[P2])(implicit om: o#arg[String, P2]#to[String, R]) = {
        om(JsonLibrary.->>.column[String](n, Node(key)))
      }
    def #> [P2, R](keyPath: Column[P2])(implicit om: o#arg[List[String], P2]#to[JValue, R]) = {
        om(JsonLibrary.#>.column[JValue](n, Node(keyPath)))
      }
    def #>>[P2, R](keyPath: Column[P2])(implicit om: o#arg[List[String], P2]#to[String, R]) = {
        om(JsonLibrary.#>>.column[String](n, Node(keyPath)))
      }

    def arrayLength[R](implicit om: o#to[Int, R]) = om(JsonLibrary.arrayLength.column(n))
    def arrayElements[R](implicit om: o#to[JValue, R]) = om(JsonLibrary.arrayElements.column(n))
    def objectKeys[R](implicit om: o#to[String, R]) = om(JsonLibrary.objectKeys.column(n))
  }

  //////////////////////////////////////////////////////////////////

  class JsonTypeMapper(jsonMethods: JsonMethods[T]) extends TypeMapperDelegate[JValue] with BaseTypeMapper[JValue] {
    import jsonMethods._

    def apply(v1: BasicProfile): TypeMapperDelegate[JValue] = this

    //----------------------------------------------------------
    def zero = null

    def sqlType = java.sql.Types.OTHER

    def sqlTypeName = "json"

    def setValue(v: JValue, p: PositionedParameters) = p.setObject(mkPgObject(v), sqlType)

    def setOption(v: Option[JValue], p: PositionedParameters) = p.setObjectOption(v.map(mkPgObject), sqlType)

    def nextValue(r: PositionedResult) = r.nextStringOption().map(parse(_)).getOrElse(zero)

    def updateValue(v: JValue, r: PositionedResult) = r.updateObject(mkPgObject(v))

    override def valueToSQLLiteral(v: JValue) = pretty(render(v))

    ///
    private def mkPgObject(v: JValue) = {
      val obj = new PGobject
      obj.setType(sqlTypeName)
      obj.setValue(compact(render(v)))
      obj
    }
  }
}
