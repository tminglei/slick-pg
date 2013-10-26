package com.github.tminglei.slickpg

import com.vividsolutions.jts.io._
import com.vividsolutions.jts.geom._
import java.sql.SQLException
import scala.slick.driver.{BasicProfile, PostgresDriver}
import scala.slick.lifted._
import scala.slick.ast.Library.{SqlFunction, SqlOperator}
import scala.slick.ast.{LiteralNode, Node}
import scala.slick.session.{PositionedResult, PositionedParameters}

trait PostGISSupport { driver: PostgresDriver =>

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
    implicit def geometryColumnExtensionMethods[G1 <: Geometry](c: Column[G1])(
      implicit tm: GeometryTypeMapper[G1]) = {
    		new GeometryColumnExtensionMethods[G1, G1](c)
    	}
    implicit def geometryOptionColumnExtensionMethods[G1 <: Geometry](c: Column[Option[G1]])(
      implicit tm: GeometryTypeMapper[G1]) = {
    		new GeometryColumnExtensionMethods[G1, Option[G1]](c)
    	}
  }

  trait PostGISAssistants extends PostGISImplicits {
    /** Geometry Constructors */
    def geomFromText[P, R](wkt: Column[P], srid: Option[Int] = None)(
      implicit om: OptionMapperDSL.arg[String, P]#to[Geometry, R]) = srid match {
        case Some(srid) => om(PostGISLibrary.GeomFromText.column[Geometry](Node(wkt), LiteralNode(srid)))
        case None   => om(PostGISLibrary.GeomFromText.column[Geometry](Node(wkt)))
      }
    def geomFromWKB[P, R](wkb: Column[P], srid: Option[Int] = None)(
      implicit om: OptionMapperDSL.arg[Array[Byte], P]#to[Geometry, R]) = srid match {
        case Some(srid) => om(PostGISLibrary.GeomFromWKB.column[Geometry](Node(wkb), LiteralNode(srid)))
        case None   => om(PostGISLibrary.GeomFromWKB.column[Geometry](Node(wkb)))
      }
    def geomFromEWKT[P, R](ewkt: Column[P])(
      implicit om: OptionMapperDSL.arg[String, P]#to[Geometry, R]) = {
        om(PostGISLibrary.GeomFromEWKT.column[Geometry](Node(ewkt)))
      }
    def geomFromEWKB[P, R](ewkb: Column[P])(
      implicit om: OptionMapperDSL.arg[Array[Byte], P]#to[Geometry, R]) = {
        om(PostGISLibrary.GeomFromEWKB.column[Geometry](Node(ewkb)))
      }
    def geomFromGML[P, R](gml: Column[P], srid: Option[Int] = None)(
      implicit om: OptionMapperDSL.arg[String, P]#to[Geometry, R]) = srid match {
        case Some(srid) => om(PostGISLibrary.GeomFromGML.column[Geometry](Node(gml), LiteralNode(srid)))
        case None   => om(PostGISLibrary.GeomFromGML.column[Geometry](Node(gml)))
      }
    def geomFromKML[P, R](kml: Column[P])(
      implicit om: OptionMapperDSL.arg[String, P]#to[Geometry, R]) = {
        om(PostGISLibrary.GeomFromKML.column[Geometry](Node(kml)))
      }
    def geomFromGeoJSON[P, R](json: Column[P])(
      implicit om: OptionMapperDSL.arg[String, P]#to[Geometry, R]) = {
        om(PostGISLibrary.GeomFromGeoJSON.column[Geometry](Node(json)))
      }
    def makeBox[G1 <: Geometry, G2 <: Geometry](lowLeftPoint: Column[G1], upRightPoint: Column[G2]) = {
        PostGISLibrary.MakeBox.column[Geometry](Node(lowLeftPoint), Node(upRightPoint))
      }
    def makePoint[P1, P2, R](x: Column[P1], y: Column[P2], z: Option[Double] = None, m: Option[Double] = None)(
      implicit om: OptionMapperDSL.arg[Double, P1]#arg[Double, P2]#to[Geometry, R]) = (z, m) match {
        case (Some(z), Some(m)) => om(PostGISLibrary.MakePoint.column[Geometry](Node(x), Node(y), LiteralNode(z), LiteralNode(m)))
        case (Some(z), None) => om(PostGISLibrary.MakePoint.column[Geometry](Node(x), Node(y), LiteralNode(z)))
        case (None, Some(m)) => om(PostGISLibrary.MakePointM.column[Geometry](Node(x), Node(y), Node(m)))
        case (None, None) => om(PostGISLibrary.MakePoint.column[Geometry](Node(x), Node(y)))
      }
  }

  //////////////////////////////////////////////////////////////////////////////////

  object PostGISLibrary {
    /** Geometry Operators */
    val BoxIntersects = new SqlOperator("&&")
    val BoxIntersects3D = new SqlOperator("&&&")
    val BoxContains = new SqlOperator("~")
    val BoxContainedBy = new SqlOperator("@")
//    val BoxEquals = new SqlOperator("=")  // it's not necessary
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

    /** Geometry Constructors */
    val GeomFromText = new SqlFunction("ST_GeomFromText")
    val GeomFromWKB = new SqlFunction("ST_GeomFromWKB")
    val GeomFromEWKT = new SqlFunction("ST_GeomFromEWKT")
    val GeomFromEWKB = new SqlFunction("ST_GeomFromEWKB")
    val GeomFromGML = new SqlFunction("ST_GeomFromGML")
    val GeomFromKML = new SqlFunction("ST_GeomFromKML")
    val GeomFromGeoJSON = new SqlFunction("ST_GeomFromGeoJSON")
    val MakeBox = new SqlFunction("ST_MakeBox2D")
    val MakePoint = new SqlFunction("ST_MakePoint")
    val MakePointM = new SqlFunction("ST_MakePointM")

    /** Geometry Accessors */
    val GeometryType = new SqlFunction("ST_GeometryType")
    val SRID = new SqlFunction("ST_SRID")
    val IsValid = new SqlFunction("ST_IsValid")
    val IsClosed = new SqlFunction("ST_IsClosed")
    val IsCollection = new SqlFunction("ST_IsCollection")
    val IsEmpty = new SqlFunction("ST_IsEmpty")
    val IsRing = new SqlFunction("ST_IsRing")
    val IsSimple = new SqlFunction("ST_IsSimple")
    val Area = new SqlFunction("ST_Area")
    val Boundary = new SqlFunction("ST_Boundary")
    val Dimension = new SqlFunction("ST_Dimension")
    val CoordDim = new SqlFunction("ST_CoordDim")
    val NDims = new SqlFunction("ST_NDims")
    val NPoints = new SqlFunction("ST_NPoints")
    val NRings = new SqlFunction("ST_NRings")

    /** Geometry Outputs */
    val AsBinary = new SqlFunction("ST_AsBinary")
    val AsText = new SqlFunction("ST_AsText")
    val AsLatLonText = new SqlFunction("ST_AsLatLonText")
    val AsEWKB = new SqlFunction("ST_AsEWKB")
    val AsEWKT = new SqlFunction("ST_AsEWKT")
    val AsHEXEWKB = new SqlFunction("ST_AsHEXEWKB")
    val AsGeoJSON = new SqlFunction("ST_AsGeoJSON")
    val AsGeoHash = new SqlFunction("ST_GeoHash")
    val AsGML = new SqlFunction("ST_AsGML")
    val AsKML = new SqlFunction("ST_AsKML")
    val AsSVG = new SqlFunction("ST_AsSVG")
    val AsX3D = new SqlFunction("ST_AsX3D")

    /** Spatial Relationships */
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

    /** Spatial Measurements */
    val Azimuth = new SqlFunction("ST_Azimuth")
    val Centroid = new SqlFunction("ST_Centroid")
    val ClosestPoint = new SqlFunction("ST_ClosestPoint")
    val PointOnSurface = new SqlFunction("ST_PointOnSurface")
    val Project = new SqlFunction("ST_Project")
    val Length = new SqlFunction("ST_Length")
    val Length3D = new SqlFunction("ST_3DLength")
    val Perimeter = new SqlFunction("ST_Perimeter")
    val Distance = new SqlFunction("ST_Distance")
    val DistanceSphere = new SqlFunction("ST_Distance_Sphere")
    val MaxDistance = new SqlFunction("ST_MaxDistance")
    val HausdorffDistance = new SqlFunction("ST_HausdorffDistance")
    val LongestLine = new SqlFunction("ST_LongestLine")
    val ShortestLine = new SqlFunction("ST_ShortestLine")

    /** Geometry Processing */
    val SetSRID = new SqlFunction("ST_SetSRID")
    val Transform = new SqlFunction("ST_Transform")
    val Simplify = new SqlFunction("ST_Simplify")
    val RemoveRepeatedPoints = new SqlFunction("ST_RemoveRepeatedPoints")
    val SimplifyPreserveTopology = new SqlFunction("ST_SimplifyPreserveTopology")
    val Difference = new SqlFunction("ST_Difference")
    val SymDifference = new SqlFunction("ST_SymDifference")
    val Intersection = new SqlFunction("ST_Intersection")
    val SharedPaths = new SqlFunction("ST_SharedPaths")
    val Split = new SqlFunction("ST_Split")
    val MinBoundingCircle = new SqlFunction("ST_MinimumBoundingCircle")

    val Buffer = new SqlFunction("ST_Buffer")
    val Multi = new SqlFunction("ST_Multi")
    val LineMerge = new SqlFunction("ST_LineMerge")
    val CollectionExtract = new SqlFunction("ST_CollectionExtract")
    val CollectionHomogenize = new SqlFunction("ST_CollectionHomogenize")
    val AddPoint = new SqlFunction("ST_AddPoint")
    val SetPoint = new SqlFunction("ST_SetPoint")
    val RemovePoint = new SqlFunction("ST_RemovePoint")
    val Reverse = new SqlFunction("ST_Reverse")
    val Scale = new SqlFunction("ST_Scale")
    val Segmentize = new SqlFunction("ST_Segmentize")
    val Snap = new SqlFunction("ST_Snap")
    val Translate = new SqlFunction("ST_Translate")
  }

  /** Extension methods for hstore Columns */
  class GeometryColumnExtensionMethods[G1 <: Geometry, P1](val c: Column[P1]) extends ExtensionMethods[G1, P1] with PostGISImplicits {
    /** Geometry Operators */
    def @&&[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxIntersects.column(n, Node(geom)))
    	}
    def @&&&[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxIntersects3D.column(n, Node(geom)))
    	}
    def @>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxContains.column(n, Node(geom)))
    	}
    def <@[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxContainedBy.column(n, Node(geom)))
    	}
    def <->[P2, R](geom: Column[P2])(implicit om: o#to[Double, R]) = {
    		om(PostGISLibrary.PointDistance.column(n, Node(geom)))
    	}
    def <#>[P2, R](geom: Column[P2])(implicit om: o#to[Double, R]) = {
    		om(PostGISLibrary.BoxDistance.column(n, Node(geom)))
    	}

    def &<[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
    		om(PostGISLibrary.BoxLooseLeft.column(n, Node(geom)))
    	}
    def <<[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.BoxStrictLeft.column(n, Node(geom)))
      }
    def &<|[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.BoxLooseBelow.column(n, Node(geom)))
      }
    def <<|[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.BoxStrictBelow.column(n, Node(geom)))
      }
    def &>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.BoxLooseRight.column(n, Node(geom)))
      }
    def >>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.BoxStrictRight.column(n, Node(geom)))
      }
    def |&>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.BoxLooseAbove.column(n, Node(geom)))
      }
    def |>>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.BoxStrictAbove.column(n, Node(geom)))
      }

    /** Geometry Accessors */
    def geomType[R](implicit om: o#to[String, R]) = {
        om(PostGISLibrary.GeometryType.column(n))
      }
    def srid[R](implicit om: o#to[Int, R]) = {
        om(PostGISLibrary.SRID.column(n))
      }
    def isValid[R](implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.IsValid.column(n))
      }
    def isClosed[R](implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.IsClosed.column(n))
      }
    def isCollection[R](implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.IsCollection.column(n))
      }
    def isEmpty[R](implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.IsEmpty.column(n))
      }
    def isRing[R](implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.IsRing.column(n))
      }
    def isSimple[R](implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.IsSimple.column(n))
      }
    def hasArc[R](implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.HasArc.column[Boolean](n))
      }
    def area[R](implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.Area.column(n))
      }
    def boundary[R](implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Boundary.column(n))
      }
    def dimension[R](implicit om: o#to[Int, R]) = {
        om(PostGISLibrary.Dimension.column(n))
      }
    def coordDim[R](implicit om: o#to[Int, R]) = {
        om(PostGISLibrary.CoordDim.column(n))
      }
    def nDims[R](implicit om: o#to[Int, R]) = {
        om(PostGISLibrary.NDims.column(n))
      }
    def nPoints[R](implicit om: o#to[Int, R]) = {
        om(PostGISLibrary.NPoints.column(n))
      }
    def nRings[R](implicit om: o#to[Int, R]) = {
        om(PostGISLibrary.NRings.column(n))
      }

    /** Geometry Outputs */
    def asBinary[R](NDRorXDR: Option[String] = None)(implicit om: o#to[Array[Byte], R]) =
      NDRorXDR match {
        case Some(endian) => om(PostGISLibrary.AsBinary.column(n, LiteralNode(endian)))
        case None   => om(PostGISLibrary.AsBinary.column(n))
      }
    def asText[R](implicit om: o#to[String, R]) = {
        om(PostGISLibrary.AsText.column(n))
      }
    def asLatLonText[R](format: Option[String] = None)(implicit om: o#to[String, R]) =
      format match {
        case Some(fmt) => om(PostGISLibrary.AsLatLonText.column(n, LiteralNode(fmt)))
        case None   => om(PostGISLibrary.AsLatLonText.column(n))
      }
    def asEWKB[R](NDRorXDR: Option[String] = None)(implicit om: o#to[Array[Byte], R]) =
      NDRorXDR match {
        case Some(endian) => om(PostGISLibrary.AsEWKB.column(n, LiteralNode(endian)))
        case None   => om(PostGISLibrary.AsEWKB.column(n))
      }
    def asEWKT[R](implicit om: o#to[String, R]) = {
        om(PostGISLibrary.AsEWKT.column(n))
      }
    def asHEXEWKB[R](NDRorXDR: Option[String] = None)(implicit om: o#to[String, R]) =
      NDRorXDR match {
        case Some(endian) => om(PostGISLibrary.AsHEXEWKB.column(n, LiteralNode(endian)))
        case None   => om(PostGISLibrary.AsHEXEWKB.column(n))
      }
    def asGeoJSON[R](maxDigits: Column[Int] = ConstColumn(15), options: Column[Int] = ConstColumn(0),
                    geoJsonVer: Option[Int] = None)(implicit om: o#to[String, R]) =
      geoJsonVer match {
        case Some(ver) => om(PostGISLibrary.AsGeoJSON.column(LiteralNode(ver), n, Node(maxDigits), Node(options)))
        case None   => om(PostGISLibrary.AsGeoJSON.column(n, Node(maxDigits), Node(options)))
      }
    def asGeoHash[R](maxChars: Option[Int] = None)(implicit om: o#to[String, R]) =
      maxChars match {
        case Some(charNum) => om(PostGISLibrary.AsHEXEWKB.column(n, LiteralNode(charNum)))
        case None   => om(PostGISLibrary.AsHEXEWKB.column(n))
      }
    def asGML[R](maxDigits: Column[Int] = ConstColumn(15), options: Column[Int] = ConstColumn(0),
                version: Option[Int] = None,  nPrefix: Option[String] = None)(implicit om: o#to[String, R]) =
      (version, nPrefix) match {
        case (Some(ver), Some(prefix)) => om(PostGISLibrary.AsGML.column(LiteralNode(ver), n, Node(maxDigits), Node(options), LiteralNode(prefix)))
        case (Some(ver), None) => om(PostGISLibrary.AsGML.column(LiteralNode(ver), n, Node(maxDigits), Node(options)))
        case (_, _)   => om(PostGISLibrary.AsGML.column(n, Node(maxDigits), Node(options)))
      }
    def asKML[R](maxDigits: Column[Int] = ConstColumn(15), version: Option[Int] = None,  nPrefix: Option[String] = None)(implicit om: o#to[String, R]) =
      (version, nPrefix) match {
        case (Some(ver), Some(prefix)) => om(PostGISLibrary.AsKML.column(LiteralNode(ver), n, Node(maxDigits), LiteralNode(prefix)))
        case (Some(ver), None) => om(PostGISLibrary.AsKML.column(LiteralNode(ver), n, Node(maxDigits)))
        case (_, _)   => om(PostGISLibrary.AsKML.column(n, Node(maxDigits)))
      }
    def asSVG[R](rel: Column[Int] = ConstColumn(0), maxDigits: Column[Int] = ConstColumn(15))(
      implicit om: o#to[String, R]) = {
        om(PostGISLibrary.AsSVG.column(n, Node(rel), Node(maxDigits)))
      }
    def asX3D[R](maxDigits: Column[Int] = ConstColumn(15), options: Column[Int] = ConstColumn(0))(
      implicit om: o#to[String, R]) = {
        om(PostGISLibrary.AsX3D.column(n, Node(maxDigits), Node(options)))
      }

    /** Spatial Relationships */
    def gEquals[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.Equals.column(n, Node(geom)))
      }
    def orderingEquals[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.OrderingEquals.column(n, Node(geom)))
      }
    def overlaps[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.Overlaps.column(n, Node(geom)))
      }
    def intersects[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.Intersects.column(n, Node(geom)))
      }
    def crosses[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.Crosses.column(n, Node(geom)))
      }
    def disjoint[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.Disjoint.column(n, Node(geom)))
      }
    def contains[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.Contains.column(n, Node(geom)))
      }
    def containsProperly[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.ContainsProperly.column(n, Node(geom)))
      }
    def within[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.Within.column(n, Node(geom)))
      }
    def dWithin[P2, R](geom: Column[P2], distance: Column[Double])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.DWithin.column(n, Node(geom), Node(distance)))
      }
    def dFullyWithin[P2, R](geom: Column[P2], distance: Column[Double])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.DFullyWithin.column(n, Node(geom), Node(distance)))
      }
    def touches[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.Touches.column(n, Node(geom)))
      }
    def relate[P2, R](geom: Column[P2], matrixPattern: Column[String])(implicit om: o#to[Boolean, R]) = {
        om(PostGISLibrary.Relate.column(n, Node(geom), Node(matrixPattern)))
      }
    def relatePattern[P2, R](geom: Column[P2], boundaryNodeRule: Option[Int] = None)(implicit om: o#to[String, R]) =
      boundaryNodeRule match {
        case Some(rule) => om(PostGISLibrary.Relate.column(n, Node(geom), LiteralNode(rule)))
        case None    => om(PostGISLibrary.Relate.column(n, Node(geom)))
      }

    /** Spatial Measurements */
    def azimuth[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.Azimuth.column(n, Node(geom)))
      }
    def centroid[R](implicit om: o#to[Point, R]) = {
        om(PostGISLibrary.Centroid.column[Point](n))
      }
    def closestPoint[P2, R](geom: Column[P2])(implicit om: o#to[Point, R]) = {
        om(PostGISLibrary.ClosestPoint.column(n, Node(geom)))
      }
    def pointOnSurface[R](implicit om: o#to[Point, R]) = {
        om(PostGISLibrary.PointOnSurface.column[Point](n))
      }
    def project[R](distance: Column[Float], azimuth: Column[Float])(implicit om: o#to[Point, R]) = {
        om(PostGISLibrary.Project.column[Point](n, Node(distance), Node(azimuth)))
      }
    def length[R](implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.Length.column[Float](n))
      }
    def length3d[R](implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.Length3D.column[Float](n))
      }
    def perimeter[R](implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.Perimeter.column[Float](n))
      }
    def distance[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.Distance.column(n, Node(geom)))
      }
    def distanceSphere[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.DistanceSphere.column(n, Node(geom)))
      }
    def maxDistance[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om(PostGISLibrary.MaxDistance.column(n, Node(geom)))
      }
    def hausdorffDistance[P2, R](geom: Column[P2], densifyFrac: Option[Float] = None)(implicit om: o#to[Float, R]) =
      densifyFrac match {
        case Some(denFrac) => om(PostGISLibrary.HausdorffDistance.column(n, Node(geom), LiteralNode(denFrac)))
        case None   => om(PostGISLibrary.HausdorffDistance.column(n, Node(geom)))
      }
    def longestLine[P2, R](geom: Column[P2])(implicit om: o#to[LineString, R]) = {
        om(PostGISLibrary.LongestLine.column[LineString](n, Node(geom)))
      }
    def shortestLine[P2, R](geom: Column[P2])(implicit om: o#to[LineString, R]) = {
        om(PostGISLibrary.ShortestLine.column[LineString](n, Node(geom)))
      }

    /** Geometry Processing */
    def setSRID[R](srid: Column[Int])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.SetSRID.column[Geometry](n, Node(srid)))
      }
    def transform[R](srid: Column[Int])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Transform.column[Geometry](n, Node(srid)))
      }
    def simplify[R](tolerance: Column[Float])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Simplify.column[Geometry](n, Node(tolerance)))
      }
    def removeRepeatedPoints[R](implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.RemoveRepeatedPoints.column[Geometry](n))
      }
    def simplifyPreserveTopology[R](tolerance: Column[Float])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.SimplifyPreserveTopology.column[Geometry](n, Node(tolerance)))
      }
    def difference[P2, R](geom: Column[P2])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Difference.column[Geometry](n, Node(geom)))
      }
    def symDifference[P2, R](geom: Column[P2])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.SymDifference.column[Geometry](n, Node(geom)))
      }
    def intersection[P2, R](geom: Column[P2])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Intersection.column[Geometry](n, Node(geom)))
      }
    def sharedPaths[P2, R](geom: Column[P2])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.SharedPaths.column[Geometry](n, Node(geom)))
      }
    def split[P2, R](blade: Column[P2])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Split.column[Geometry](n, Node(blade)))
      }
    def minBoundingCircle[R](segNumPerQtrCircle: Column[Int] = ConstColumn(48))(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.MinBoundingCircle.column[Geometry](n, Node(segNumPerQtrCircle)))
      }

    def buffer[R](radius: Column[Float], bufferStyles: Option[String] = None)(implicit om: o#to[Geometry, R]) =
      bufferStyles match {
        case Some(styles) => om(PostGISLibrary.Buffer.column[Geometry](n, Node(radius), Node(styles)))
        case None   =>  om(PostGISLibrary.Buffer.column[Geometry](n, Node(radius)))
      }
    def multi[R](implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Multi.column[Geometry](n))
      }
    def lineMerge[R](implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.LineMerge.column[Geometry](n))
      }
    def collectionExtract[R](tpe: Column[Int])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.CollectionExtract.column[Geometry](n, Node(tpe)))
      }
    def collectionHomogenize[R](implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.CollectionHomogenize.column[Geometry](n))
      }
    def addPoint[P2, R](point: Column[P2], position: Option[Int] = None)(implicit om: o#to[Geometry, R]) =
      position match {
        case Some(pos) => om(PostGISLibrary.AddPoint.column[Geometry](n, Node(point), Node(pos)))
        case None   =>  om(PostGISLibrary.AddPoint.column[Geometry](n, Node(point)))
      }
    def setPoint[P2, R](point: Column[P2], position: Column[Int])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.SetPoint.column[Geometry](n, Node(position), Node(point)))
      }
    def removePoint[R](offset: Column[Int])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.RemovePoint.column[Geometry](n, Node(offset)))
      }
    def reverse[R](implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Reverse.column[Geometry](n))
      }
    def scale[R](xFactor: Column[Float], yFactor: Column[Float], zFactor: Option[Float] = None)(implicit om: o#to[Geometry, R]) =
      zFactor match {
        case Some(zFac) => om(PostGISLibrary.Scale.column[Geometry](n, Node(xFactor), Node(yFactor), LiteralNode(zFac)))
        case None   =>  om(PostGISLibrary.Scale.column[Geometry](n, Node(xFactor), Node(yFactor)))
      }
    def segmentize[R](maxLength: Column[Float])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Segmentize.column[Geometry](n, Node(maxLength)))
      }
    def snap[P2, R](reference: Column[P2], tolerance: Column[Float])(implicit om: o#to[Geometry, R]) = {
        om(PostGISLibrary.Snap.column[Geometry](n, Node(reference), Node(tolerance)))
      }
    def translate[R](deltaX: Column[Float], deltaY: Column[Float], deltaZ: Option[Float] = None)(implicit om: o#to[Geometry, R]) =
      deltaZ match {
        case Some(deltaZ) => om(PostGISLibrary.Translate.column[Geometry](n, Node(deltaX), Node(deltaY), LiteralNode(deltaZ)))
        case None   =>  om(PostGISLibrary.Translate.column[Geometry](n, Node(deltaX), Node(deltaY)))
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

    def nextValue(r: PositionedResult): T = r.nextStringOption().map(fromLiteral).getOrElse(zero)

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
