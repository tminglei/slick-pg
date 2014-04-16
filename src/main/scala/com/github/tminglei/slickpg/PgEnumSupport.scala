package com.github.tminglei.slickpg

import org.postgresql.util.PGobject
import scala.slick.jdbc.JdbcType
import scala.slick.driver.PostgresDriver
import scala.slick.lifted.Column
import scala.reflect.ClassTag
import java.sql.{PreparedStatement, ResultSet}

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
             implicit tag: ClassTag[List[enumObject.Value]]): JdbcType[List[enumObject.Value]] = {

    new DriverJdbcType[List[enumObject.Value]] {

      override val classTag: ClassTag[List[enumObject.Value]] = tag

      override def sqlType: Int = java.sql.Types.ARRAY

      override def sqlTypeName: String = s"$sqlEnumTypeName ARRAY"

      override def getValue(r: ResultSet, idx: Int): List[enumObject.Value] = {
        val value = r.getArray(idx)
        if (r.wasNull) Nil
        else value.getArray.asInstanceOf[Array[String]]
          .map(s => enumObject.withName(s)).toList
      }

      override def setValue(vList: List[enumObject.Value], p: PreparedStatement, idx: Int): Unit = ???

      override def updateValue(vList: List[enumObject.Value], r: ResultSet, idx: Int): Unit = ???

      override def hasLiteralForm: Boolean = false

      override def valueToSQLLiteral(vList: List[enumObject.Value]) = ???
    }
  }

  def createEnumJdbcType[T <: Enumeration](sqlEnumTypeName: String, enumObject: T)(
             implicit tag: ClassTag[enumObject.Value]): JdbcType[enumObject.Value] = {

    new DriverJdbcType[enumObject.Value] {

      override val classTag: ClassTag[enumObject.Value] = tag

      override def sqlType: Int = java.sql.Types.OTHER

      override def sqlTypeName: String = sqlEnumTypeName

      override def getValue(r: ResultSet, idx: Int): enumObject.Value = {
        val value = r.getString(idx)
        if (r.wasNull) null.asInstanceOf[enumObject.Value] else enumObject.withName(value)
      }

      override def setValue(v: enumObject.Value, p: PreparedStatement, idx: Int): Unit = p.setObject(idx, mkPgObject(v), sqlType)

      override def updateValue(v: enumObject.Value, r: ResultSet, idx: Int): Unit = r.updateObject(idx, mkPgObject(v))

      override def hasLiteralForm: Boolean = true

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
  import scala.slick.jdbc.{StaticQuery => Q, Invoker}

  def buildCreateSql[T <: Enumeration](sqlTypeName: String, enumObject: T): Invoker[Int] = {
    // `toStream` to prevent re-ordering after `map(e => s"'${e.toString}'")`
    val enumValuesString = enumObject.values.toStream.map(e => s"'${e.toString}'").mkString(",")
    Q[Int] + s"create type $sqlTypeName as enum ($enumValuesString)"
  }
  
  def buildDropSql(sqlTypeName: String): Invoker[Int] = Q[Int] + s"drop type $sqlTypeName"
}