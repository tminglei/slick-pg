package com.github.tminglei.slickpg
package utils

import scala.reflect.ClassTag
import slick.jdbc.{SetParameter, PositionedParameters}

object PlainSQLUtils {
  import SimpleArrayUtils._

  def mkSetParameter[T](typeName: String, toStr: (T => String) = (v: T) => v.toString,
            sqlType: Int = java.sql.Types.OTHER): SetParameter[T] =
    new SetParameter[T] {
      def apply(v: T, pp: PositionedParameters) = internalSet(sqlType, typeName, Option(v), pp, toStr)
    }
  def mkOptionSetParameter[T](typeName: String, toStr: (T => String) = (v: T) => v.toString,
            sqlType: Int = java.sql.Types.OTHER): SetParameter[Option[T]] =
    new SetParameter[Option[T]] {
      def apply(v: Option[T], pp: PositionedParameters) = internalSet(sqlType, typeName, v, pp, toStr)
    }

  private def internalSet[T](sqlType: Int, typeName: String, v: Option[T], p: PositionedParameters, toStr: (T => String)) =
    v match {
      case Some(v) => p.setObject(utils.mkPGobject(typeName, toStr(v)), java.sql.Types.OTHER)
      case None    => p.setNull(sqlType)
    }

  ///
  def mkArraySetParameter[T: ClassTag](baseType: String, toStr: (T => String) = (v: T) => v.toString,
          seqToStr: Option[(Seq[T] => String)] = None): SetParameter[Seq[T]] =
    new SetParameter[Seq[T]] {
      def apply(v: Seq[T], pp: PositionedParameters) = internalSetArray(baseType, Option(v), pp, toStr, seqToStr)
    }
  def mkArrayOptionSetParameter[T: ClassTag](baseType: String, toStr: (T => String) = (v: T) => v.toString,
          seqToStr: Option[(Seq[T] => String)] = None): SetParameter[Option[Seq[T]]] =
    new SetParameter[Option[Seq[T]]] {
      def apply(v: Option[Seq[T]], pp: PositionedParameters) = internalSetArray(baseType, v, pp, toStr, seqToStr)
    }

  private def internalSetArray[T: ClassTag](baseType: String, v: Option[Seq[T]], p: PositionedParameters,
          toStr: (T => String), seqToStr: Option[(Seq[T] => String)]) = {
    val _seqToStr = seqToStr.getOrElse(mkString(toStr) _); p.pos += 1
    v match {
      case Some(vList) => p.ps.setArray(p.pos, mkArray(_seqToStr)(baseType, vList))
      case None        => p.ps.setNull(p.pos, java.sql.Types.ARRAY)
    }
  }
}
