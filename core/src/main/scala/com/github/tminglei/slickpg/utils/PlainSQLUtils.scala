package com.github.tminglei.slickpg
package utils

import scala.reflect.ClassTag
import scala.slick.jdbc.{SetParameter, PositionedParameters}

object PlainSQLUtils {
  import SimpleArrayUtils._

  def mkSetParameter[T](sqlType: String, toStr: (T => String) = (v: T) => v.toString): SetParameter[T] =
    new SetParameter[T] {
      def apply(v: T, pp: PositionedParameters) = internalSet(sqlType, Option(v), pp, toStr)
    }
  def mkOptionSetParameter[T](sqlType: String, toStr: (T => String) = (v: T) => v.toString): SetParameter[Option[T]] =
    new SetParameter[Option[T]] {
      def apply(v: Option[T], pp: PositionedParameters) = internalSet(sqlType, v, pp, toStr)
    }

  private def internalSet[T](sqlType: String, v: Option[T], p: PositionedParameters, toStr: (T => String)) = v match {
    case Some(v) => p.setObject(utils.mkPGobject(sqlType, v.toString), java.sql.Types.OTHER)
    case None    => p.setNull(java.sql.Types.OTHER)
  }

  ///
  def mkArraySetParameter[T: ClassTag](baseType: String, toStr: (T => String) = (v: T) => v.toString): SetParameter[Seq[T]] =
    new SetParameter[Seq[T]] {
      def apply(v: Seq[T], pp: PositionedParameters) = internalSetArray(baseType, Option(v), pp, toStr)
    }
  def mkArrayOptionSetParameter[T: ClassTag](baseType: String, toStr: (T => String) = (v: T) => v.toString): SetParameter[Option[Seq[T]]] =
    new SetParameter[Option[Seq[T]]] {
      def apply(v: Option[Seq[T]], pp: PositionedParameters) = internalSetArray(baseType, v, pp, toStr)
    }

  private def internalSetArray[T: ClassTag](baseType: String, v: Option[Seq[T]], p: PositionedParameters, toStr: (T => String)) = {
    p.pos += 1
    v match {
      case Some(vList) => p.ps.setArray(p.pos, mkArray(mkString[T](toStr))(baseType, vList))
      case None        => p.ps.setNull(p.pos, java.sql.Types.ARRAY)
    }
  }
}
