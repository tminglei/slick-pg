package com.github.tminglei.slickpg.date

import java.sql.{PreparedStatement, ResultSet}
import java.time.format.DateTimeFormatter
import java.time._
import java.util.Calendar

import slick.ast.FieldSymbol
import slick.jdbc.{JdbcTypesComponent, PostgresProfile}

import scala.reflect.ClassTag

/**
  * Created by minglei on 5/15/17.
  */
trait PgDateJdbcTypes extends JdbcTypesComponent { driver: PostgresProfile =>

  class GenericDateJdbcType[T: ClassTag](val sqlTypeName: String, val sqlType: Int) extends DriverJdbcType[T] {

    classTag.runtimeClass match {
      case clazz if clazz == classOf[LocalDate] =>
      case clazz if clazz == classOf[LocalTime] =>
      case clazz if clazz == classOf[LocalDateTime] =>
      case clazz if clazz == classOf[OffsetDateTime] =>
      case clazz if clazz == classOf[Calendar] =>
      case clazz => throw new IllegalArgumentException("Unsupported type: " + clazz.getName)
    }
    ///---

    override def sqlTypeName(sym: Option[FieldSymbol]): String = sqlTypeName

    override def getValue(r: ResultSet, idx: Int): T = {
      val value = classTag.runtimeClass match {
        case clazz if clazz == classOf[LocalDate]     => r.getObject(idx, classOf[LocalDate])
        case clazz if clazz == classOf[LocalTime]     => r.getObject(idx, classOf[LocalTime])
        case clazz if clazz == classOf[LocalDateTime] => r.getObject(idx, classOf[LocalDateTime])
        case clazz if clazz == classOf[OffsetDateTime] => r.getObject(idx, classOf[OffsetDateTime])
        case clazz if clazz == classOf[Calendar]      => r.getObject(idx, classOf[Calendar])
        case clazz => throw new IllegalArgumentException("Unsupported type: " + clazz.getName)
      }
      if (r.wasNull) null.asInstanceOf[T] else value.asInstanceOf[T]
    }

    override def setValue(v: T, p: PreparedStatement, idx: Int): Unit =
      if (v == null) p.setNull(idx, sqlType) else classTag.runtimeClass match {
        case clazz if clazz == classOf[Calendar] => p.setObject(idx, v.asInstanceOf[Calendar].toInstant.atOffset(ZoneOffset.UTC))
        case clazz                               => p.setObject(idx, v)
      }

    override def updateValue(v: T, r: ResultSet, idx: Int): Unit =
      if (v == null) r.updateNull(idx) else classTag.runtimeClass match {
        case clazz if clazz == classOf[Calendar] => r.updateObject(idx, v.asInstanceOf[Calendar].toInstant.atOffset(ZoneOffset.UTC))
        case clazz                               => r.updateObject(idx, v)
      }

    override def valueToSQLLiteral(v: T) = if (v == null) "NULL" else classTag.runtimeClass match {
      case clazz if clazz == classOf[LocalDate]     => s"'${v.asInstanceOf[LocalDate].format(DateTimeFormatter.ISO_LOCAL_DATE)}'"
      case clazz if clazz == classOf[LocalTime]     => s"'${v.asInstanceOf[LocalTime].format(DateTimeFormatter.ISO_LOCAL_TIME)}'"
      case clazz if clazz == classOf[LocalDateTime] => s"'${v.asInstanceOf[LocalDateTime].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}'"
      case clazz if clazz == classOf[OffsetDateTime] => s"'${v.asInstanceOf[OffsetDateTime].format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}'"
      case clazz if clazz == classOf[Calendar]      => s"'${v.asInstanceOf[Calendar].toInstant.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}'"
      case clazz => throw new IllegalArgumentException("Unsupported type: " + clazz.getName)
    }
  }
}
