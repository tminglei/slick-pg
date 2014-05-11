package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import com.vividsolutions.jts.geom._
import scala.slick.lifted.Column
import scala.reflect.ClassTag
import com.vividsolutions.jts.io.{WKBReader, WKBWriter, WKTReader, WKTWriter}
import java.sql.{PreparedStatement, ResultSet}

trait PgPostGISSupport extends geom.PgPostGISExtensions { driver: PostgresDriver =>

  type GEOMETRY   = Geometry
  type POINT      = Point
  type LINESTRING = LineString
  type POLYGON    = Polygon
  type GEOMETRYCOLLECTION = GeometryCollection

  trait PostGISImplicits {
    implicit val geometryTypeMapper = new GeometryJdbcType[Geometry]
    implicit val pointTypeMapper = new GeometryJdbcType[Point]
    implicit val polygonTypeMapper = new GeometryJdbcType[Polygon]
    implicit val lineStringTypeMapper = new GeometryJdbcType[LineString]
    implicit val linearRingTypeMapper = new GeometryJdbcType[LinearRing]
    implicit val geometryCollectionTypeMapper = new GeometryJdbcType[GeometryCollection]
    implicit val multiPointTypeMapper = new GeometryJdbcType[MultiPoint]
    implicit val multiPolygonTypeMapper = new GeometryJdbcType[MultiPolygon]
    implicit val multiLineStringTypeMapper = new GeometryJdbcType[MultiLineString]

    ///
    implicit def geometryColumnExtensionMethods[G1 <: Geometry](c: Column[G1]) = new GeometryColumnExtensionMethods[G1, G1](c)
    implicit def geometryOptionColumnExtensionMethods[G1 <: Geometry](c: Column[Option[G1]]) = new GeometryColumnExtensionMethods[G1, Option[G1]](c)
  }

  ////// geometry jdbc type
  class GeometryJdbcType[T <: Geometry](implicit override val classTag: ClassTag[T]) extends DriverJdbcType[T] {
    import PgPostGISSupportUtils._

    override def sqlType: Int = java.sql.Types.OTHER

    override def sqlTypeName: String = "geometry"

    override def getValue(r: ResultSet, idx: Int): T = {
      val value = r.getString(idx)
      if (r.wasNull) null.asInstanceOf[T] else fromLiteral[T](value)
    }

    override def setValue(v: T, p: PreparedStatement, idx: Int): Unit = p.setBytes(idx, toBytes(v))

    override def updateValue(v: T, r: ResultSet, idx: Int): Unit = r.updateBytes(idx, toBytes(v))

    override def hasLiteralForm: Boolean = false

    override def valueToSQLLiteral(v: T) = if(v eq null) "NULL" else s"'${toLiteral(v)}'"
  }
}

object PgPostGISSupportUtils {
  private val wktWriterHolder = new ThreadLocal[WKTWriter]
  private val wktReaderHolder = new ThreadLocal[WKTReader]
  private val wkbWriterHolder = new ThreadLocal[WKBWriter]
  private val wkbReaderHolder = new ThreadLocal[WKBReader]

  def toLiteral(geom: Geometry): String = {
    if (wktWriterHolder.get == null) wktWriterHolder.set(new WKTWriter())
    wktWriterHolder.get.write(geom)
  }
  def fromLiteral[T](value: String): T = {
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

  def toBytes(geom: Geometry): Array[Byte] = {
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