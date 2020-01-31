package com.github.tminglei.slickpg;

import org.locationtech.jts.geom.*;

public interface PgGeographyTypes {

    class GeographyFactory extends GeometryFactory {
        @Override
        public GeogPoint createPoint(CoordinateSequence coordinates) {
            return new GeogPoint(coordinates, this);
        }
        @Override
        public LineString createLineString(CoordinateSequence coordinates) {
            return new GeogLineString(coordinates, this);
        }
        @Override
        public LinearRing createLinearRing(CoordinateSequence coordinates) {
            return new GeogLinearRing(coordinates, this);
        }
        @Override
        public MultiPoint createMultiPoint(Point[] points) {
            return new GeogMultiPoint(points, this);
        }
        @Override
        public Polygon createPolygon(LinearRing shell, LinearRing[] holes) {
            return new GeogPolygon(shell, holes, this);
        }
        @Override
        public MultiLineString createMultiLineString() {
            return new GeogMultiLineString(null, this);
        }
        @Override
        public MultiLineString createMultiLineString(LineString[] lineStrings) {
            return new GeogMultiLineString(lineStrings, this);
        }
        @Override
        public MultiPolygon createMultiPolygon() {
            return new GeogMultiPolygon(null, this);
        }
        @Override
        public MultiPolygon createMultiPolygon(Polygon[] polygons) {
            return new GeogMultiPolygon(polygons, this);
        }
        @Override
        public GeometryCollection createGeometryCollection() {
            return new GeographyCollection(null, this);
        }
        @Override
        public GeometryCollection createGeometryCollection(Geometry[] geometries) {
            return new GeographyCollection(geometries, this);
        }
    }

    ///---

    interface Geography {}

    class GeogPoint extends Point implements Geography {
        public GeogPoint(CoordinateSequence coordinates, GeographyFactory factory) {
            super(coordinates, factory);
        }
    }
    class GeogLineString extends LineString implements Geography {
        public GeogLineString(CoordinateSequence points, GeographyFactory factory) {
            super(points, factory);
        }
    }
    class GeogLinearRing extends LinearRing implements Geography {
        public GeogLinearRing(CoordinateSequence points, GeographyFactory factory) {
            super(points, factory);
        }
    }
    class GeogMultiPoint extends MultiPoint implements Geography {
        public GeogMultiPoint(Point[] points, GeographyFactory factory) {
            super(points, factory);
        }
    }
    class GeogPolygon extends Polygon implements Geography {
        public GeogPolygon(LinearRing shell, LinearRing[] holes, GeographyFactory factory) {
            super(shell, holes, factory);
        }
    }
    class GeogMultiLineString extends MultiLineString implements Geography {
        public GeogMultiLineString(LineString[] lineStrings, GeographyFactory factory) {
            super(lineStrings, factory);
        }
    }
    class GeogMultiPolygon extends MultiPolygon implements Geography {
        public GeogMultiPolygon(Polygon[] polygons, GeographyFactory factory) {
            super(polygons, factory);
        }
    }
    class GeographyCollection extends GeometryCollection implements Geography {
        public GeographyCollection(Geometry[] geometries, GeographyFactory factory) {
            super(geometries, factory);
        }
    }
}
