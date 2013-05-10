package org.slick.driver
package pg

import com.vividsolutions.jts.io._
import com.vividsolutions.jts.geom._
import java.sql.SQLException
import scala.slick.driver.{BasicProfile, PostgresDriver}
import scala.slick.lifted.{BaseTypeMapper, TypeMapperDelegate, ExtensionMethods, Column}
import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.ast.{LiteralNode, Node}
import scala.slick.session.{PositionedResult, PositionedParameters}

trait PostGISSupport { driver: PostgresDriver =>

  trait GeometryTypeMapperImplicits {
    implicit val geometryTypeMapper = new GeometryTypeMapper[Geometry]
    implicit val pointTypeMapper = new GeometryTypeMapper[Point]
    implicit val polygonTypeMapper = new GeometryTypeMapper[Polygon]
    implicit val lineStringTypeMapper = new GeometryTypeMapper[LineString]
    implicit val linearRingTypeMapper = new GeometryTypeMapper[LinearRing]
    implicit val geometryCollectionTypeMapper = new GeometryTypeMapper[GeometryCollection]
    implicit val multiPointTypeMapper = new GeometryTypeMapper[MultiPoint]
    implicit val multiPolygonTypeMapper = new GeometryTypeMapper[MultiPolygon]
    implicit val multiLineStringTypeMapper = new GeometryTypeMapper[MultiLineString]
  }
  
  trait PostGISImplicits extends GeometryTypeMapperImplicits {
    implicit def geometryColumnExtensionMethods[G1 <: Geometry](c: Column[G1])(
      implicit tm: GeometryTypeMapper[G1]) = {
    		new GeometryColumnExtensionMethods[G1, G1](c)
    	}
    implicit def geometryOptionColumnExtensionMethods[G1 <: Geometry](c: Column[Option[G1]])(
      implicit tm: GeometryTypeMapper[G1]) = {
    		new GeometryColumnExtensionMethods[G1, Option[G1]](c)
    	}
  }

  //////////////////////////////////////////////////////////////////////////////////

  object PostGISLibrary {
    /** Geometry Operators */
    val BoxIntersects = new SqlOperator("&&")
    val BoxIntersects3D = new SqlOperator("&&&")
    val BoxContains = new SqlOperator("~")
    val BoxContainedBy = new SqlOperator("@")
    val BoxEquals = new SqlOperator("=")
    val PointDistance = new SqlOperator("<->")
    val BoxDistance = new SqlOperator("<#>")

    val BoxLooseLeft = new SqlOperator("&<")
    val BoxStrictLeft = new SqlOperator("<<")
    val BoxLooseBelow = new SqlOperator("&<|")
    val BoxStrictBelow = new SqlOperator("<<|")
    val BoxLooseRight = new SqlOperator("&>")
    val BoxStrictRight = new SqlOperator(">>")
    val BoxLooseAbove = new SqlOperator("|&>")
    val BoxStrictAbove = new SqlOperator("|>>")

    /** Spatial Relationships and Measurements */
    val Azimuth = new SqlFunction("ST_Azimuth")
    val Centroid = new SqlFunction("ST_Centroid")
    val Area = new SqlFunction("ST_Area")
    val ClosestPoint = new SqlFunction("ST_ClosestPoint")
    val PointOnSurface = new SqlFunction("ST_PointOnSurface")
    val Project = new SqlFunction("ST_Project")
    val HasArc = new SqlFunction("ST_HasArc")
    val Equals = new SqlFunction("ST_Equals")
    val OrderingEquals = new SqlFunction("ST_OrderingEquals")

    val Overlaps = new SqlFunction("ST_Overlaps")
    val Intersects = new SqlFunction("ST_Intersects")
    val Crosses = new SqlFunction("ST_Crosses")
    val Disjoint = new SqlFunction("ST_Disjoint")
    val Contains = new SqlFunction("ST_Contains")
    val ContainsProperly = new SqlFunction("ST_ContainsProperly")
    val Within = new SqlFunction("ST_Within")
    val DWithin = new SqlFunction("ST_DWithin")
    val DFullyWithin = new SqlFunction("ST_DFullyWithin")
    val Touches = new SqlFunction("ST_Touches")
    val Relate = new SqlFunction("ST_Relate")

    val Length = new SqlFunction("ST_Length")
    val Perimeter = new SqlFunction("ST_Perimeter")
    val Distance = new SqlFunction("ST_Distance")
    val DistanceSphere = new SqlFunction("ST_Distance_Sphere")
    val MaxDistance = new SqlFunction("ST_MaxDistance")
    val HausdorffDistance = new SqlFunction("ST_HausdorffDistance")
    val LongestLine = new SqlFunction("ST_LongestLine")
    val ShortestLine = new SqlFunction("ST_ShortestLine")

    /** Geometry Processing */
    val Simplify = new SqlFunction("ST_Simplify")
    val RemoveRepeatedPoints = new SqlFunction("ST_RemoveRepeatedPoints")
    val SimplifyPreserveTopology = new SqlFunction("ST_SimplifyPreserveTopology")
    val Difference = new SqlFunction("ST_Difference")
    val SymDifference = new SqlFunction("ST_SymDifference")
    val Intersection = new SqlFunction("ST_Intersection")
    val SharedPaths = new SqlFunction("ST_SharedPaths")
    val Split = new SqlFunction("ST_Split")
    val LineToCurve = new SqlFunction("ST_LineToCurve")
    val CurveToLine = new SqlFunction("ST_CurveToLine")
    val OffsetCurve = new SqlFunction("ST_OffsetCurve")
    val UnaryUnion = new SqlFunction("ST_UnaryUnion")

    val MinBoundingCircle = new SqlFunction("ST_MinimumBoundingCircle")
  }

  /** Extension methods for hstore Columns */
  class GeometryColumnExtensionMethods[G1, P1](val c: Column[P1]) 
  						extends ExtensionMethods[G1, P1] with GeometryTypeMapperImplicits {
    /** Geometry Operators */
    def @&&[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxIntersects.column(n, Node(geom)))
    	}
    def @&&&[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxIntersects3D.column(n, Node(geom)))
    	}
    def @>[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxContains.column(n, Node(geom)))
    	}
    def <@[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxContainedBy.column(n, Node(geom)))
    	}
    def <->[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
    		om(PostGISLibrary.PointDistance.column(n, Node(geom)))
    	}
    def <#>[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxDistance.column(n, Node(geom)))
    	}

    def &<[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxLooseLeft.column(n, Node(geom)))
    	}
    def <<[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.BoxStrictLeft.column(n, Node(geom)))
      }
    def &<|[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.BoxLooseBelow.column(n, Node(geom)))
      }
    def <<|[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.BoxStrictBelow.column(n, Node(geom)))
      }
    def &>[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.BoxLooseRight.column(n, Node(geom)))
      }
    def >>[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.BoxStrictRight.column(n, Node(geom)))
      }
    def |&>[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.BoxLooseAbove.column(n, Node(geom)))
      }
    def |>>[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.BoxStrictAbove.column(n, Node(geom)))
      }

    /** Spatial Relationships and Measurements */
    def azimuth[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Float, R]) = {
        om(PostGISLibrary.Azimuth.column(n, Node(geom)))
      }
    def centroid[R](implicit om: o#to[Point, R]) = {
        om(PostGISLibrary.Centroid.column[Point](n))
      }
    def area[R](implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.Area.column(n))
      }
    def closestPoint[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Float, R]) = {
        om(PostGISLibrary.ClosestPoint.column(n, Node(geom)))
      }
    def pointOnSurface[R](implicit om: o#to[Point, R]) = {
        om(PostGISLibrary.PointOnSurface.column[Point](n))
      }
    def project[R](distance: Column[Float], azimuth: Column[Float])(implicit om: o#to[Point, R]) = {
        om(PostGISLibrary.Project.column[Point](n, Node(distance), Node(azimuth)))
      }
    def hasArc[R](implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.HasArc.column[Boolean](n))
      }
    def equals[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.Equals.column(n, Node(geom)))
      }
    def orderingEquals[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.OrderingEquals.column(n, Node(geom)))
      }

    def overlaps[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.Overlaps.column(n, Node(geom)))
      }
    def intersects[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.Intersects.column(n, Node(geom)))
      }
    def crosses[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.Crosses.column(n, Node(geom)))
      }
    def disjoint[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.Disjoint.column(n, Node(geom)))
      }
    def contains[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.Contains.column(n, Node(geom)))
      }
    def containsProperly[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.ContainsProperly.column(n, Node(geom)))
      }
    def within[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.Within.column(n, Node(geom)))
      }
    def dWithin[G2, P2, R](geom: Column[P2], distance: Column[Double])(
      implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.DWithin.column(n, Node(geom), Node(distance)))
      }
    def dFullyWithin[G2, P2, R](geom: Column[P2], distance: Column[Double])(
      implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.DFullyWithin.column(n, Node(geom), Node(distance)))
      }
    def touches[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.Touches.column(n, Node(geom)))
      }
    def relate[G2, P2, R](geom: Column[P2], intersectionMatrixPattern: Column[String])(
      implicit om: o#arg[G2, P2]#to[Boolean, R]) = {
        om(PostGISLibrary.Relate.column(n, Node(geom), Node(intersectionMatrixPattern)))
      }
    def relatePattern[G2, P2, R](geom: Column[P2], boundaryNodeRule: Option[Int] = None)(
      implicit om: o#arg[G2, P2]#to[String, R]) = boundaryNodeRule match {
        case Some(rule) => om(PostGISLibrary.Relate.column(n, Node(geom), LiteralNode(rule)))
        case None    => om(PostGISLibrary.Relate.column(n, Node(geom)))
      }

    def length[R](implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.Length.column[Float](n))
      }
    def perimeter[R](implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.Perimeter.column[Float](n))
      }
    def distance[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Float, R]) = {
        om(PostGISLibrary.Distance.column(n, Node(geom)))
      }
    def distanceSphere[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Float, R]) = {
        om(PostGISLibrary.DistanceSphere.column(n, Node(geom)))
      }
    def maxDistance[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Float, R]) = {
        om(PostGISLibrary.MaxDistance.column(n, Node(geom)))
      }
    def hausdorffDistance[G2, P2, R](geom: Column[P2], densifyFrac: Option[Float] = None)(
      implicit om: o#arg[G2, P2]#to[Float, R]) = densifyFrac match {
        case Some(denFrac) => om(PostGISLibrary.HausdorffDistance.column(n, Node(geom), LiteralNode(denFrac)))
        case None       => om(PostGISLibrary.HausdorffDistance.column(n, Node(geom)))
      }
    def longestLine[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[LineString, R]) = {
        om(PostGISLibrary.LongestLine.column[LineString](n, Node(geom)))
      }
    def shortestLine[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[LineString, R]) = {
        om(PostGISLibrary.ShortestLine.column[LineString](n, Node(geom)))
      }

    /** Geometry Processing */
    def simplify[R](tolerance: Column[Float])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Simplify.column[Geometry](n, Node(tolerance)))
      }
    def removeRepeatedPoints[R](implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.RemoveRepeatedPoints.column[Geometry](n))
      }
    def simplifyPreserveTopology[R](tolerance: Column[Float])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.SimplifyPreserveTopology.column[Geometry](n, Node(tolerance)))
      }
    def difference[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Geometry, R]) = {
        om(PostGISLibrary.Difference.column[Geometry](n, Node(geom)))
      }
    def symDifference[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Geometry, R]) = {
        om(PostGISLibrary.SymDifference.column[Geometry](n, Node(geom)))
      }
    def intersection[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Geometry, R]) = {
        om(PostGISLibrary.Intersection.column[Geometry](n, Node(geom)))
      }
    def sharedPaths[G2, P2, R](geom: Column[P2])(implicit om: o#arg[G2, P2]#to[Geometry, R]) = {
        om(PostGISLibrary.SharedPaths.column[Geometry](n, Node(geom)))
      }
    def split[G2, P2, R](blade: Column[P2])(implicit om: o#arg[G2, P2]#to[Geometry, R]) = {
        om(PostGISLibrary.Split.column[Geometry](n, Node(blade)))
      }
    def lineToCurve[R](implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.LineToCurve.column[Geometry](n))
      }
    def curveToLine(segmentsPerQtrCircle: Column[Int]) = {
        PostGISLibrary.CurveToLine.column[G1](n, Node(segmentsPerQtrCircle))
      }
    def offsetCurve(signedDistance: Column[Float], styleParameters: String = "") = {
        PostGISLibrary.OffsetCurve.column[G1](n, Node(signedDistance), LiteralNode(styleParameters))
      }
    def unaryUnion[R](implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.UnaryUnion.column[Geometry](n))
      }
    def minBoundingCircle[R](numSegmentsPerQtrCircle: Int = 48)(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.MinBoundingCircle.column[Geometry](n, LiteralNode(numSegmentsPerQtrCircle)))
      }
  }

  //////////////////////////////////////////////////////////////////////////////////

  class GeometryTypeMapper[T <: Geometry] extends TypeMapperDelegate[T] with BaseTypeMapper[T] {
    
    def apply(v1: BasicProfile): TypeMapperDelegate[T] = this

    //--------------------------------------------------------
    def zero: T = null.asInstanceOf[T]

    def sqlType: Int = java.sql.Types.OTHER

    def sqlTypeName: String = "geometry"

    def setValue(v: T, p: PositionedParameters) = p.setBytes(toBytes(v))

    def setOption(v: Option[T], p: PositionedParameters) = if (v.isDefined) setValue(v.get, p) else p.setNull(sqlType)

    def nextValue(r: PositionedResult): T = r.nextStringOption().map(fromLiteral _).getOrElse(zero)

    def updateValue(v: T, r: PositionedResult) = r.updateBytes(toBytes(v))

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
        val index = value.indexOf(';', 5); // srid prefix length is 5
        if (index == -1) {
          throw new SQLException("Error parsing Geometry - SRID not delimited with ';' ");
        } else {
          val srid = Integer.parseInt(value.substring(0, index))
          val wkt = value.substring(index + 1)
          (srid, wkt)
        }
      } else (-1, value)
    }
  }
}
