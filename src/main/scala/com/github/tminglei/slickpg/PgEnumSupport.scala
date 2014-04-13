package com.github.tminglei.slickpg

import org.postgresql.util.PGobject
import scala.slick.jdbc.{PositionedResult, PositionedParameters}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.reflect.ClassTag

trait PgEnumSupport extends enums.PgEnumExtensions { driver: PostgresDriver =>
  
  def createEnumColumnExtensionMethodsBuilder[T <: Enumeration](enumObject: T)(
      implicit tm: JdbcType[enumObject.Value], tm1: JdbcType[List[enumObject.Value]]) = 
    (c: Column[enumObject.Value]) => {
      new EnumColumnExtensionMethods[enumObject.Value, enumObject.Value](c)(tm, tm1)
    }
  def createEnumOptionColumnExtensionMethodsBuilder[T <: Enumeration](enumObject: T)(
      implicit tm: JdbcType[enumObject.Value], tm1: JdbcType[List[enumObject.Value]]) = 
    (c: Column[Option[enumObject.Value]]) => {
      new EnumColumnExtensionMethods[enumObject.Value, Option[enumObject.Value]](c)(tm, tm1)
    }

  //-----------------------------------------------------------------------------------
  
  def createEnumListJdbcType[T <: Enumeration](sqlEnumTypeName: String, enumObject: T)(
             implicit tag: ClassTag[enumObject.Value]): JdbcType[List[enumObject.Value]] with BaseTypedType[List[enumObject.Value]] = {

    new JdbcType[List[enumObject.Value]] with BaseTypedType[List[enumObject.Value]] {

      def scalaType: ScalaType[List[enumObject.Value]] = ScalaBaseType[List[enumObject.Value]]

      def zero: List[enumObject.Value] = Nil

      def sqlType: Int = java.sql.Types.ARRAY

      def sqlTypeName: String = s"$sqlEnumTypeName ARRAY"

      def setValue(v: List[enumObject.Value], p: PositionedParameters) = ???

      def setOption(v: Option[List[enumObject.Value]], p: PositionedParameters) = ???

      def nextValue(r: PositionedResult): List[enumObject.Value] =
        r.nextObjectOption().map(_.asInstanceOf[java.sql.Array])
          .map(_.getArray.asInstanceOf[Array[String]].map(s => enumObject.withName(s)).toList)
          .getOrElse(zero)

      def updateValue(v: List[enumObject.Value], r: PositionedResult) = ???

      def hasLiteralForm: Boolean = false

      override def valueToSQLLiteral(v: List[enumObject.Value]) = ???
    }
  }

  def createEnumJdbcType[T <: Enumeration](sqlEnumTypeName: String, enumObject: T)(
             implicit tag: ClassTag[enumObject.Value]): JdbcType[enumObject.Value] with BaseTypedType[enumObject.Value] = {

    new JdbcType[enumObject.Value] with BaseTypedType[enumObject.Value] {

      def scalaType: ScalaType[enumObject.Value] = ScalaBaseType[enumObject.Value]

      def zero: enumObject.Value = null.asInstanceOf[enumObject.Value]

      def sqlType: Int = java.sql.Types.OTHER

      def sqlTypeName: String = sqlEnumTypeName

      def setValue(v: enumObject.Value, p: PositionedParameters) = p.setObject(mkPgObject(v), sqlType)

      def setOption(v: Option[enumObject.Value], p: PositionedParameters) = p.setObjectOption(v.map(mkPgObject), sqlType)

      def nextValue(r: PositionedResult): enumObject.Value =
        r.nextStringOption().map(s => enumObject.withName(s)).getOrElse(zero)

      def updateValue(v: enumObject.Value, r: PositionedResult) = r.updateObject(mkPgObject(v))

      def hasLiteralForm: Boolean = true

      override def valueToSQLLiteral(v: enumObject.Value) = if (v eq null) null else v.toString

      ///
      private def mkPgObject(v: enumObject.Value) = {
        val obj = new PGobject
        obj.setType(sqlTypeName)
        obj.setValue(valueToSQLLiteral(v))
        obj
      }
    }
  }
}

object PgEnumSupportUtils {
  import scala.slick.jdbc.{StaticQuery => Q, UnitInvoker}

  def buildCreateSql[T <: Enumeration](sqlTypeName: String, enumObject: T): UnitInvoker[Int] = {
    // `toStream` to prevent re-ordering after `map(e => s"'${e.toString}'")`
    val enumValuesString = enumObject.values.toStream.map(e => s"'${e.toString}'").mkString(",")
    Q[Int] + s"create type $sqlTypeName as enum ($enumValuesString)"
  }
  
  def buildDropSql(sqlTypeName: String): UnitInvoker[Int] = Q[Int] + s"drop type $sqlTypeName"
}