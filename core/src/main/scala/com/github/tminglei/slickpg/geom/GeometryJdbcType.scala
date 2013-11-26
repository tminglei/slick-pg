package com.github.tminglei.slickpg
package geom

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.{WKBReader, WKBWriter, WKTReader, WKTWriter}
import scala.slick.jdbc.{PositionedResult, PositionedParameters, JdbcType}
import scala.slick.ast.{ScalaBaseType, ScalaType, BaseTypedType}
import scala.reflect.ClassTag

class GeometryJdbcType[T <: Geometry](implicit tag: ClassTag[T]) extends JdbcType[T] with BaseTypedType[T] {

  def scalaType: ScalaType[T] = ScalaBaseType[T]

  def zero: T = null.asInstanceOf[T]

  def sqlType: Int = java.sql.Types.OTHER

  def sqlTypeName: String = "geometry"

  def setValue(v: T, p: PositionedParameters) = p.setBytes(toBytes(v))

  def setOption(v: Option[T], p: PositionedParameters) = if (v.isDefined) setValue(v.get, p) else p.setNull(sqlType)

  def nextValue(r: PositionedResult): T = r.nextStringOption().map(fromLiteral).getOrElse(zero)

  def updateValue(v: T, r: PositionedResult) = r.updateBytes(toBytes(v))

  def hasLiteralForm: Boolean = true

  override def valueToSQLLiteral(v: T) = toLiteral(v)

  //////
  private val wktWriterHolder = new ThreadLocal[WKTWriter]
  private val wktReaderHolder = new ThreadLocal[WKTReader]
  private val wkbWriterHolder = new ThreadLocal[WKBWriter]
  private val wkbReaderHolder = new ThreadLocal[WKBReader]

  private def toLiteral(geom: Geometry): String = {
    if (wktWriterHolder.get == null) wktWriterHolder.set(new WKTWriter())
    wktWriterHolder.get.write(geom)
  }
  private def fromLiteral(value: String): T = {
    if (wktReaderHolder.get == null) wktReaderHolder.set(new WKTReader())
    splitRSIDAndWKT(value) match {
      case (srid, wkt) => {
        val geom =
          if (wkt.startsWith("00") || wkt.startsWith("01"))
            fromBytes(WKBReader.hexToBytes(wkt))
          else wktReaderHolder.get.read(wkt)

        if (srid != -1) geom.setSRID(srid)
        geom.asInstanceOf[T]
      }
    }
  }

  private def toBytes(geom: Geometry): Array[Byte] = {
    if (wkbWriterHolder.get == null) wkbWriterHolder.set(new WKBWriter(2, true))
    wkbWriterHolder.get.write(geom)
  }
  private def fromBytes[T](bytes: Array[Byte]): T = {
    if (wkbReaderHolder.get == null) wkbReaderHolder.set(new WKBReader())
    wkbReaderHolder.get.read(bytes).asInstanceOf[T]
  }

  /** copy from [[org.postgis.PGgeometry#splitSRID]] */
  private def splitRSIDAndWKT(value: String): (Int, String) = {
    if (value.startsWith("SRID=")) {
      val index = value.indexOf(';', 5) // srid prefix length is 5
      if (index == -1) {
        throw new java.sql.SQLException("Error parsing Geometry - SRID not delimited with ';' ")
      } else {
        val srid = Integer.parseInt(value.substring(0, index))
        val wkt = value.substring(index + 1)
        (srid, wkt)
      }
    } else (-1, value)
  }
}
