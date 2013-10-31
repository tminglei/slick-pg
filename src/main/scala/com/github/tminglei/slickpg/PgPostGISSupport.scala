package com.github.tminglei.slickpg

import scala.slick.driver.PostgresDriver
import com.vividsolutions.jts.geom._
import scala.slick.lifted.Column

trait PgPostGISSupport extends geom.PgPostGISExtensions { driver: PostgresDriver =>
  import geom.GeometryTypeMapper

  type GEOMETRY   = Geometry
  type POINT      = Point
  type LINESTRING = LineString
  type POLYGON    = Polygon
  type GEOMETRYCOLLECTION = GeometryCollection

  trait PostGISImplicits {
    implicit val geometryTypeMapper = new GeometryTypeMapper[Geometry]
    implicit val pointTypeMapper = new GeometryTypeMapper[Point]
    implicit val polygonTypeMapper = new GeometryTypeMapper[Polygon]
    implicit val lineStringTypeMapper = new GeometryTypeMapper[LineString]
    implicit val linearRingTypeMapper = new GeometryTypeMapper[LinearRing]
    implicit val geometryCollectionTypeMapper = new GeometryTypeMapper[GeometryCollection]
    implicit val multiPointTypeMapper = new GeometryTypeMapper[MultiPoint]
    implicit val multiPolygonTypeMapper = new GeometryTypeMapper[MultiPolygon]
    implicit val multiLineStringTypeMapper = new GeometryTypeMapper[MultiLineString]

    ///
    implicit def geometryColumnExtensionMethods[G1 <: Geometry](c: Column[G1]) = new GeometryColumnExtensionMethods[G1, G1](c)
    implicit def geometryOptionColumnExtensionMethods[G1 <: Geometry](c: Column[Option[G1]]) = new GeometryColumnExtensionMethods[G1, Option[G1]](c)
  }
}
