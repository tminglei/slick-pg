package com.github.tminglei.slickpg

import slick.ast.FieldSymbol
import slick.jdbc.JdbcType
import slick.driver.PostgresDriver
import slick.profile.RelationalProfile.ColumnOption.Length
import scala.reflect.ClassTag
import java.sql.{PreparedStatement, ResultSet}

trait PgEnumSupport extends enums.PgEnumExtensions with array.PgArrayJdbcTypes { driver: PostgresDriver =>
  import driver.api._
  import PgEnumSupportUtils.sqlName

  def createEnumColumnExtensionMethodsBuilder[T <: Enumeration](enumObject: T)(
      implicit tm: JdbcType[enumObject.Value], tm1: JdbcType[List[enumObject.Value]]) =
    (c: Rep[enumObject.Value]) => {
      new EnumColumnExtensionMethods[enumObject.Value, enumObject.Value](c)(tm, tm1)
    }
  def createEnumOptionColumnExtensionMethodsBuilder[T <: Enumeration](enumObject: T)(
      implicit tm: JdbcType[enumObject.Value], tm1: JdbcType[List[enumObject.Value]]) =
    (c: Rep[Option[enumObject.Value]]) => {
      new EnumColumnExtensionMethods[enumObject.Value, Option[enumObject.Value]](c)(tm, tm1)
    }

  //-----------------------------------------------------------------------------------

  def createEnumListJdbcType[T <: Enumeration](sqlEnumTypeName: String, enumObject: T, quoteName: Boolean = false)(
             implicit tag: ClassTag[List[enumObject.Value]]): JdbcType[List[enumObject.Value]] = {
    new AdvancedArrayJdbcType[enumObject.Value](sqlName(sqlEnumTypeName, quoteName),
      fromString = s => utils.SimpleArrayUtils.fromString(s1 => enumObject.withName(s1))(s).orNull,
      mkString = v => utils.SimpleArrayUtils.mkString[Any](_.toString)(v)
    ).to(_.toList)
  }

  def createEnumJdbcType[T <: Enumeration](sqlEnumTypeName: String, enumObject: T, quoteName: Boolean = false)(
             implicit tag: ClassTag[enumObject.Value]): JdbcType[enumObject.Value] = {

    new DriverJdbcType[enumObject.Value] {

      override val classTag: ClassTag[enumObject.Value] = tag

      override def sqlType: Int = java.sql.Types.OTHER

      override def sqlTypeName(sym: Option[FieldSymbol]): String = sqlName(sqlEnumTypeName, quoteName)

      override def getValue(r: ResultSet, idx: Int): enumObject.Value = {
        val value = r.getString(idx)
        if (r.wasNull) null.asInstanceOf[enumObject.Value] else enumObject.withName(value)
      }

      override def setValue(v: enumObject.Value, p: PreparedStatement, idx: Int): Unit = p.setObject(idx, toStr(v), sqlType)

      override def updateValue(v: enumObject.Value, r: ResultSet, idx: Int): Unit = r.updateObject(idx, toStr(v), sqlType)

      override def hasLiteralForm: Boolean = true

      override def valueToSQLLiteral(v: enumObject.Value) = if (v eq null) "NULL" else s"'$v'"

      ///
      private def toStr(v: enumObject.Value) = if (v eq null) null else v.toString
    }
  }
}

trait PgCustomEnumSupport extends enums.PgEnumExtensions with array.PgArrayJdbcTypes { driver: PostgresDriver =>

  import driver.api._
  import PgEnumSupportUtils.sqlName

  def createEnumColumnExtensionMethodsBuilder[T](implicit tm: JdbcType[T], tm1: JdbcType[List[T]]) =
    (c: Rep[T]) => {
      new EnumColumnExtensionMethods[T, T](c)(tm, tm1)
    }

  def createEnumOptionColumnExtensionMethodsBuilder[T](implicit tm: JdbcType[T], tm1: JdbcType[List[T]]) =
    (c: Rep[Option[T]]) => {
      new EnumColumnExtensionMethods[T, Option[T]](c)(tm, tm1)
    }

  //-----------------------------------------------------------------------------------

  def createEnumListJdbcType[T](sqlEnumTypeName: String, enumToString: (T => String), stringToEnum: (String => T), quoteName: Boolean = false)
                               (implicit tag: ClassTag[T]): JdbcType[List[T]] = {
    new AdvancedArrayJdbcType[T](sqlName(sqlEnumTypeName, quoteName),
      fromString = s => utils.SimpleArrayUtils.fromString(s1 => stringToEnum(s1))(s).orNull,
      mkString = v => utils.SimpleArrayUtils.mkString[T](enumToString)(v)
    ).to(_.toList)
  }

  def createEnumJdbcType[T](sqlEnumTypeName: String, enumToString: (T => String), stringToEnum: (String => T), quoteName: Boolean = false)
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

      override def valueToSQLLiteral(v: T) = if (v == null) "NULL" else s"'$v'"

      private def toStr(v: T) = if (v == null) null else enumToString(v)
    }
  }
}

object PgEnumSupportUtils {
  import slick.driver.PostgresDriver.api._

  def sqlName(sqlTypeName: String, quoteName: Boolean) = {
    if (quoteName) '"' + sqlTypeName + '"' else sqlTypeName.toLowerCase
  }

  def buildCreateSql[T <: Enumeration](sqlTypeName: String, enumObject: T, quoteName: Boolean = false) = {
    // `toStream` to prevent re-ordering after `map(e => s"'${e.toString}'")`
    val enumValuesString = enumObject.values.toStream.map(e => s"'$e'").mkString(",")
    sqlu"create type #${sqlName(sqlTypeName, quoteName)} as enum (#$enumValuesString)"
  }

  def buildCreateCustomSql[T](sqlTypeName: String, enumValues: Seq[String], quoteName: Boolean = false) = {
    // `toStream` to prevent re-ordering after `map(e => s"'${e.toString}'")`
    val enumValuesString = enumValues.mkString("'", "', '", "'")
    sqlu"create type #${sqlName(sqlTypeName, quoteName)} as enum (#$enumValuesString)"
  }

  def buildDropSql(sqlTypeName: String, quoteName: Boolean = false) = {
    sqlu"drop type #${sqlName(sqlTypeName, quoteName)}"
  }
}
