package com.github.tminglei.slickpg

import slick.ast.FieldSymbol
import slick.jdbc.{JdbcType, PostgresProfile}
import slick.sql.SqlAction

import scala.reflect.ClassTag
import java.sql.{PreparedStatement, ResultSet}

trait PgEnumSupport extends enums.PgEnumExtensions with array.PgArrayJdbcTypes { driver: PostgresProfile =>
  import driver.api._
  import PgEnumSupportUtils.sqlName

  def createEnumColumnExtensionMethodsBuilder[T <: Enumeration](enumObject: T)(
      implicit tm: JdbcType[enumObject.Value], tm1: JdbcType[List[enumObject.Value]]) =
    (c: Rep[enumObject.Value]) => {
      new EnumColumnExtensionMethods[enumObject.Value, enumObject.Value](c)(tm, tm1)
    }
  def createEnumColumnExtensionMethodsBuilder[T](implicit tm: JdbcType[T], tm1: JdbcType[List[T]]) =
    (c: Rep[T]) => {
      new EnumColumnExtensionMethods[T, T](c)(tm, tm1)
    }

  def createEnumOptionColumnExtensionMethodsBuilder[T <: Enumeration](enumObject: T)(
      implicit tm: JdbcType[enumObject.Value], tm1: JdbcType[List[enumObject.Value]]) =
    (c: Rep[Option[enumObject.Value]]) => {
      new EnumColumnExtensionMethods[enumObject.Value, Option[enumObject.Value]](c)(tm, tm1)
    }
  def createEnumOptionColumnExtensionMethodsBuilder[T](implicit tm: JdbcType[T], tm1: JdbcType[List[T]]) =
    (c: Rep[Option[T]]) => {
      new EnumColumnExtensionMethods[T, Option[T]](c)(tm, tm1)
    }

  //-----------------------------------------------------------------------------------

  def createEnumListJdbcType[T <: Enumeration](sqlEnumTypeName: String, enumObject: T, quoteName: Boolean = false)
                               (implicit tag: ClassTag[List[enumObject.Value]]): JdbcType[List[enumObject.Value]] = {
    createEnumListJdbcType[enumObject.Value](sqlEnumTypeName, _.toString, s => enumObject.withName(s), quoteName)
  }

  def createEnumListJdbcType[T](sqlEnumTypeName: String, enumToString: (T => String), stringToEnum: (String => T), quoteName: Boolean)
                               (implicit tag: ClassTag[T]): JdbcType[List[T]] = {
    new AdvancedArrayJdbcType[T](sqlName(sqlEnumTypeName, quoteName),
      fromString = s => utils.SimpleArrayUtils.fromString(s1 => stringToEnum(s1))(s).orNull,
      mkString = v => utils.SimpleArrayUtils.mkString[T](enumToString)(v)
    ).to(_.toList)
  }

  def createEnumJdbcType[T <: Enumeration](sqlEnumTypeName: String, enumObject: T, quoteName: Boolean = false)
                           (implicit tag: ClassTag[enumObject.Value]): JdbcType[enumObject.Value] = {
    createEnumJdbcType[enumObject.Value](sqlEnumTypeName, _.toString, s => enumObject.withName(s), quoteName)
  }
  def createEnumJdbcType[T](sqlEnumTypeName: String, enumToString: (T => String), stringToEnum: (String => T), quoteName: Boolean)
                           (implicit tag: ClassTag[T]): JdbcType[T] = {

    new DriverJdbcType[T] {

      override val classTag: ClassTag[T] = tag

      override def sqlType: Int = java.sql.Types.OTHER

      override def sqlTypeName(sym: Option[FieldSymbol]): String = sqlName(sqlEnumTypeName, quoteName)

      override def getValue(r: ResultSet, idx: Int): T = {
        val value = r.getString(idx)
        if (r.wasNull) null.asInstanceOf[T] else stringToEnum(value)
      }

      override def setValue(v: T, p: PreparedStatement, idx: Int): Unit = p.setObject(idx, toStr(v), sqlType)

      override def updateValue(v: T, r: ResultSet, idx: Int): Unit = r.updateObject(idx, toStr(v), sqlType)

      override def hasLiteralForm: Boolean = true

      override def valueToSQLLiteral(v: T) = if (v == null) "NULL" else s"'${enumToString(v)}'"

      private def toStr(v: T) = if (v == null) null else enumToString(v)
    }
  }
}

object PgEnumSupportUtils {
  import PostgresProfile.api._

  def sqlName(sqlTypeName: String, quoteName: Boolean) = {
    if (quoteName) '"' + sqlTypeName + '"' else sqlTypeName.toLowerCase
  }

  def buildCreateSql[T <: Enumeration](sqlTypeName: String, enumObject: T, quoteName: Boolean = false): SqlAction[Int, NoStream, Effect] = {
    // `toStream` to prevent re-ordering after `map(_.toString)`
    buildCreateSql(sqlTypeName, enumObject.values.toStream.map(_.toString), quoteName)
  }

  def buildCreateSql(sqlTypeName: String, enumValues: Seq[String], quoteName: Boolean): SqlAction[Int, NoStream, Effect] = {
    val enumValuesString = enumValues.mkString("'", "', '", "'")
    sqlu"create type #${sqlName(sqlTypeName, quoteName)} as enum (#$enumValuesString)"
  }

  def buildDropSql(sqlTypeName: String, quoteName: Boolean = false) = {
    sqlu"drop type #${sqlName(sqlTypeName, quoteName)}"
  }
}
