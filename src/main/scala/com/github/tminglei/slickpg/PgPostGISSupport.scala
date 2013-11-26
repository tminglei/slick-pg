package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import com.vividsolutions.jts.geom._
import scala.slick.lifted.Column

trait PgPostGISSupport extends geom.PgPostGISExtensions { driver: PostgresDriver =>
  import geom.GeometryJdbcType

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
}
