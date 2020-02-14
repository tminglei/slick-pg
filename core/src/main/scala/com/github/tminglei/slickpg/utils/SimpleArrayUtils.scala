package com.github.tminglei.slickpg
package utils

import java.sql.ResultSet
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

  def mkString[T](toString: T => String)(value: Seq[Any]): String = {
    def escaped(str: String): String = {
      val buf = StringBuilder.newBuilder
      str.map {
        case '\'' => buf append "''"
        case ch   => buf append ch
      }
      buf.toString
    }
    def toGroupToken(vList: Seq[Any]): Token = GroupToken(Open("{") +: vList.map {
      case null => Null
      case v if v.isInstanceOf[Seq[_]] => toGroupToken(v.asInstanceOf[Seq[_]])
      case v    => Chunk(escaped(toString(v.asInstanceOf[T])))
    } :+ Close("}"))

    createString (value match {
      case null  => Null
      case vList => toGroupToken(vList)
    })
  }

  def mkArray[T : ClassTag](mkString: (Seq[T] => String))(sqlBaseType: String, vList: Seq[T]): java.sql.Array =
    new SimpleArray(sqlBaseType, vList, mkString)

  ////////////////////////////////////////////////////////////////////////////////////

  /** !!! NOTE: only used to transfer array data into driver/preparedStatement. !!! */
  private class SimpleArray[T : ClassTag](sqlBaseTypeName: String, vList: Seq[T], mkString: (Seq[T] => String)) extends java.sql.Array {

    override def getBaseTypeName = sqlBaseTypeName.replace("[]", "").trim

    override def getBaseType(): Int = ???

    override def getArray(): AnyRef = vList.toArray

    override def getArray(map: JMap[String, Class[_]]): AnyRef = ???

    override def getArray(index: Long, count: Int): AnyRef = ???

    override def getArray(index: Long, count: Int, map: JMap[String, Class[_]]): AnyRef = ???

    override def getResultSet(): ResultSet = ???

    override def getResultSet(map: JMap[String, Class[_]]): ResultSet = ???

    override def getResultSet(index: Long, count: Int): ResultSet = ???

    override def getResultSet(index: Long, count: Int, map: JMap[String, Class[_]]): ResultSet = ???

    override def free() = { /* nothing to do */ }

    override def toString = mkString(vList)
  }
}
