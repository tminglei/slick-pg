package com.github.tminglei.slickpg
package utils

import java.util.{Map => JMap}
import scala.reflect.ClassTag

object SimpleArrayUtils {
  import PgTokenHelper._
  
  def fromString[T](convert: String => T)(arrString: String): Option[Seq[T]] =
    grouping(Tokenizer.tokenize(arrString)) match {
      case Null => None
      case root => Some(getChildren(root).map {
        case Null  => null.asInstanceOf[T]
        case token => convert(getString(token, 0))
      })
    }

  def mkString[T](ToString: T => String)(value: Seq[T]): String =
    createString (value match {
      case null  => Null
      case vList => {
        val members = Open("{") +: vList.map {
            case v => if (v == null) Null else Chunk(ToString(v))
          } :+ Close("}")
        GroupToken(members)
      }
    })

  def mkArray[T : ClassTag](mkString: (Seq[T] => String))(sqlBaseType: String, vList: Seq[T]): java.sql.Array =
    new SimpleArray(sqlBaseType, vList, mkString)

  ////////////////////////////////////////////////////////////////////////////////////

  /** !!! NOTE: only used to transfer array data into driver/preparedStatement. !!! */
  private class SimpleArray[T : ClassTag](sqlBaseTypeName: String, vList: Seq[T], mkString: (Seq[T] => String)) extends java.sql.Array {

    def getBaseTypeName = sqlBaseTypeName.replaceFirst("^\"", "").replaceFirst("\"$", "")

    def getBaseType = ???

    def getArray = vList.toArray

    def getArray(map: JMap[String, Class[_]]) = ???

    def getArray(index: Long, count: Int) = ???

    def getArray(index: Long, count: Int, map: JMap[String, Class[_]]) = ???

    def getResultSet = ???

    def getResultSet(map: JMap[String, Class[_]]) = ???

    def getResultSet(index: Long, count: Int) = ???

    def getResultSet(index: Long, count: Int, map: JMap[String, Class[_]]) = ???

    def free() = { /* nothing to do */ }

    override def toString = mkString(vList)
  }
}
