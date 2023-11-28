package com.github.tminglei.slickpg
package str

import slick.ast.Library.{SqlFunction, SqlOperator}
import slick.ast.TypedType
import slick.jdbc.{JdbcTypesComponent, PostgresProfile}
import slick.lifted.ExtensionMethods

/**
  * Created by minglei on 12/12/16.
  */
trait PgStringExtensions extends JdbcTypesComponent { driver: PostgresProfile =>
  import driver.api._

  object StringLibrary {
    val ILike = new SqlOperator("ilike")
    val PosixMatchCaseSensitive = new SqlOperator("~")
    val PosixMatchCaseInsensitive = new SqlOperator("~*")
    val PosixNoMatchCaseSensitive = new SqlOperator("!~")
    val PosixNoMatchCaseInsensitive = new SqlOperator("!~*")

    val Convert = new SqlFunction("convert")
    val ConvertFrom = new SqlFunction("convert_from")
    val ConvertTo = new SqlFunction("convert_to")
    val Encode = new SqlFunction("encode")
    val Decode = new SqlFunction("decode")
  }

  class PgStringColumnExtensionMethods[P1](val c: Rep[P1]) extends ExtensionMethods[String, P1] {
    protected def b1Type = implicitly[TypedType[String]]

    def ilike[P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
        om.column(StringLibrary.ILike, n, e.toNode)
      }

    def ~[P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
      om.column(StringLibrary.PosixMatchCaseSensitive, n, e.toNode)
    }

    def ~*[P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
      om.column(StringLibrary.PosixMatchCaseInsensitive, n, e.toNode)
    }

    def !~[P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
      om.column(StringLibrary.PosixNoMatchCaseSensitive, n, e.toNode)
    }

    def !~*[P2, R](e: Rep[P2])(implicit om: o#arg[String, P2]#to[Boolean, R]) = {
      om.column(StringLibrary.PosixNoMatchCaseInsensitive, n, e.toNode)
    }

    def convertTo[R](destEncoding: Rep[String])(implicit om: o#to[Array[Byte], R]) = {
        om.column(StringLibrary.ConvertTo, n, destEncoding.toNode)
      }
    def decode[R](format: Rep[String])(implicit om: o#to[Array[Byte], R]) = {
        om.column(StringLibrary.Decode, n, format.toNode)
      }
  }

  class PgStringByteaColumnExtensionMethods[P1](val c: Rep[P1]) extends ExtensionMethods[Array[Byte], P1] {
    protected def b1Type = implicitly[TypedType[Array[Byte]]]

    def convert[R](srcEncoding: Rep[String], destEncoding: Rep[String])(implicit om: o#to[Array[Byte], R]) = {
        om.column(StringLibrary.Convert, n, srcEncoding.toNode, destEncoding.toNode)
      }
    def convertFrom[R](srcEncoding: Rep[String])(implicit om: o#to[String, R]) = {
        om.column(StringLibrary.ConvertFrom, n, srcEncoding.toNode)
      }
    def encode[R](format: Rep[String])(implicit om: o#to[String, R]) = {
        om.column(StringLibrary.Encode, n, format.toNode)
      }
  }
}
