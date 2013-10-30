package com.github.tminglei.slickpg.geom

import scala.slick.lifted.{ConstColumn, ExtensionMethods, OptionMapperDSL, Column}
import scala.slick.ast.{LiteralNode, Node}
import scala.slick.ast.Library.{SqlFunction, SqlOperator}

trait PgPostGISExtensions {

  type GEOMETRY
  type POINT <: GEOMETRY
  type LINESTRING <: GEOMETRY

  trait PostGISAssistants {
    /** Geometry Constructors */
    def geomFromText[P, R](wkt: Column[P], srid: Option[Int] = None)(implicit om: OptionMapperDSL.arg[String, P]#to[GEOMETRY, R]) =
      srid match {
        case Some(srid) => om(GeomLibrary.GeomFromText.column[GEOMETRY](Node(wkt), LiteralNode(srid)))
        case None   => om(GeomLibrary.GeomFromText.column[GEOMETRY](Node(wkt)))
      }
    def geomFromWKB[P, R](wkb: Column[P], srid: Option[Int] = None)(implicit om: OptionMapperDSL.arg[Array[Byte], P]#to[GEOMETRY, R]) = 
      srid match {
        case Some(srid) => om(GeomLibrary.GeomFromWKB.column[GEOMETRY](Node(wkb), LiteralNode(srid)))
        case None   => om(GeomLibrary.GeomFromWKB.column[GEOMETRY](Node(wkb)))
      }
    def geomFromEWKT[P, R](ewkt: Column[P])(implicit om: OptionMapperDSL.arg[String, P]#to[GEOMETRY, R]) = {
        om(GeomLibrary.GeomFromEWKT.column[GEOMETRY](Node(ewkt)))
      }
    def geomFromEWKB[P, R](ewkb: Column[P])(implicit om: OptionMapperDSL.arg[Array[Byte], P]#to[GEOMETRY, R]) = {
        om(GeomLibrary.GeomFromEWKB.column[GEOMETRY](Node(ewkb)))
      }
    def geomFromGML[P, R](gml: Column[P], srid: Option[Int] = None)(implicit om: OptionMapperDSL.arg[String, P]#to[GEOMETRY, R]) = 
      srid match {
        case Some(srid) => om(GeomLibrary.GeomFromGML.column[GEOMETRY](Node(gml), LiteralNode(srid)))
        case None   => om(GeomLibrary.GeomFromGML.column[GEOMETRY](Node(gml)))
      }
    def geomFromKML[P, R](kml: Column[P])(implicit om: OptionMapperDSL.arg[String, P]#to[GEOMETRY, R]) = {
        om(GeomLibrary.GeomFromKML.column[GEOMETRY](Node(kml)))
      }
    def geomFromGeoJSON[P, R](json: Column[P])(implicit om: OptionMapperDSL.arg[String, P]#to[GEOMETRY, R]) = {
        om(GeomLibrary.GeomFromGeoJSON.column[GEOMETRY](Node(json)))
      }
    def makeBox[G1 <: GEOMETRY, G2 <: GEOMETRY](lowLeftPoint: Column[G1], upRightPoint: Column[G2]) = {
        GeomLibrary.MakeBox.column[GEOMETRY](Node(lowLeftPoint), Node(upRightPoint))
      }
    def makePoint[P1, P2, R](x: Column[P1], y: Column[P2], z: Option[Double] = None, m: Option[Double] = None)(
            implicit om: OptionMapperDSL.arg[Double, P1]#arg[Double, P2]#to[GEOMETRY, R]) = 
      (z, m) match {
        case (Some(z), Some(m)) => om(GeomLibrary.MakePoint.column[GEOMETRY](Node(x), Node(y), LiteralNode(z), LiteralNode(m)))
        case (Some(z), None) => om(GeomLibrary.MakePoint.column[GEOMETRY](Node(x), Node(y), LiteralNode(z)))
        case (None, Some(m)) => om(GeomLibrary.MakePointM.column[GEOMETRY](Node(x), Node(y), Node(m)))
        case (None, None) => om(GeomLibrary.MakePoint.column[GEOMETRY](Node(x), Node(y)))
      }
  }

  //////////////////////////////////////////////////////////////////////////////////

  object GeomLibrary {
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
  class GeometryColumnExtensionMethods[G1 <: GEOMETRY, P1](val c: Column[P1]) extends ExtensionMethods[G1, P1] {
    /** Geometry Operators */
    def @&&[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxIntersects.column(n, Node(geom)))
      }
    def @&&&[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxIntersects3D.column(n, Node(geom)))
      }
    def @>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxContains.column(n, Node(geom)))
      }
    def <@[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxContainedBy.column(n, Node(geom)))
      }
    def <->[P2, R](geom: Column[P2])(implicit om: o#to[Double, R]) = {
        om(GeomLibrary.PointDistance.column(n, Node(geom)))
      }
    def <#>[P2, R](geom: Column[P2])(implicit om: o#to[Double, R]) = {
        om(GeomLibrary.BoxDistance.column(n, Node(geom)))
      }

    def &<[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxLooseLeft.column(n, Node(geom)))
      }
    def <<[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxStrictLeft.column(n, Node(geom)))
      }
    def &<|[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxLooseBelow.column(n, Node(geom)))
      }
    def <<|[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxStrictBelow.column(n, Node(geom)))
      }
    def &>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxLooseRight.column(n, Node(geom)))
      }
    def >>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxStrictRight.column(n, Node(geom)))
      }
    def |&>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxLooseAbove.column(n, Node(geom)))
      }
    def |>>[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.BoxStrictAbove.column(n, Node(geom)))
      }

    /** Geometry Accessors */
    def geomType[R](implicit om: o#to[String, R]) = {
        om(GeomLibrary.GeometryType.column(n))
      }
    def srid[R](implicit om: o#to[Int, R]) = {
        om(GeomLibrary.SRID.column(n))
      }
    def isValid[R](implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.IsValid.column(n))
      }
    def isClosed[R](implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.IsClosed.column(n))
      }
    def isCollection[R](implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.IsCollection.column(n))
      }
    def isEmpty[R](implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.IsEmpty.column(n))
      }
    def isRing[R](implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.IsRing.column(n))
      }
    def isSimple[R](implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.IsSimple.column(n))
      }
    def hasArc[R](implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.HasArc.column[Boolean](n))
      }
    def area[R](implicit om: o#to[Float, R]) = {
        om(GeomLibrary.Area.column(n))
      }
    def boundary[R](implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.Boundary.column[GEOMETRY](n))
      }
    def dimension[R](implicit om: o#to[Int, R]) = {
        om(GeomLibrary.Dimension.column(n))
      }
    def coordDim[R](implicit om: o#to[Int, R]) = {
        om(GeomLibrary.CoordDim.column(n))
      }
    def nDims[R](implicit om: o#to[Int, R]) = {
        om(GeomLibrary.NDims.column(n))
      }
    def nPoints[R](implicit om: o#to[Int, R]) = {
        om(GeomLibrary.NPoints.column(n))
      }
    def nRings[R](implicit om: o#to[Int, R]) = {
        om(GeomLibrary.NRings.column(n))
      }

    /** Geometry Outputs */
    def asBinary[R](NDRorXDR: Option[String] = None)(implicit om: o#to[Array[Byte], R]) =
      NDRorXDR match {
        case Some(endian) => om(GeomLibrary.AsBinary.column(n, LiteralNode(endian)))
        case None   => om(GeomLibrary.AsBinary.column(n))
      }
    def asText[R](implicit om: o#to[String, R]) = {
        om(GeomLibrary.AsText.column(n))
      }
    def asLatLonText[R](format: Option[String] = None)(implicit om: o#to[String, R]) =
      format match {
        case Some(fmt) => om(GeomLibrary.AsLatLonText.column(n, LiteralNode(fmt)))
        case None   => om(GeomLibrary.AsLatLonText.column(n))
      }
    def asEWKB[R](NDRorXDR: Option[String] = None)(implicit om: o#to[Array[Byte], R]) =
      NDRorXDR match {
        case Some(endian) => om(GeomLibrary.AsEWKB.column(n, LiteralNode(endian)))
        case None   => om(GeomLibrary.AsEWKB.column(n))
      }
    def asEWKT[R](implicit om: o#to[String, R]) = {
        om(GeomLibrary.AsEWKT.column(n))
      }
    def asHEXEWKB[R](NDRorXDR: Option[String] = None)(implicit om: o#to[String, R]) =
      NDRorXDR match {
        case Some(endian) => om(GeomLibrary.AsHEXEWKB.column(n, LiteralNode(endian)))
        case None   => om(GeomLibrary.AsHEXEWKB.column(n))
      }
    def asGeoJSON[R](maxDigits: Column[Int] = ConstColumn(15), options: Column[Int] = ConstColumn(0),
                     geoJsonVer: Option[Int] = None)(implicit om: o#to[String, R]) =
      geoJsonVer match {
        case Some(ver) => om(GeomLibrary.AsGeoJSON.column(LiteralNode(ver), n, Node(maxDigits), Node(options)))
        case None   => om(GeomLibrary.AsGeoJSON.column(n, Node(maxDigits), Node(options)))
      }
    def asGeoHash[R](maxChars: Option[Int] = None)(implicit om: o#to[String, R]) =
      maxChars match {
        case Some(charNum) => om(GeomLibrary.AsHEXEWKB.column(n, LiteralNode(charNum)))
        case None   => om(GeomLibrary.AsHEXEWKB.column(n))
      }
    def asGML[R](maxDigits: Column[Int] = ConstColumn(15), options: Column[Int] = ConstColumn(0),
                 version: Option[Int] = None,  nPrefix: Option[String] = None)(implicit om: o#to[String, R]) =
      (version, nPrefix) match {
        case (Some(ver), Some(prefix)) => om(GeomLibrary.AsGML.column(LiteralNode(ver), n, Node(maxDigits), Node(options), LiteralNode(prefix)))
        case (Some(ver), None) => om(GeomLibrary.AsGML.column(LiteralNode(ver), n, Node(maxDigits), Node(options)))
        case (_, _)   => om(GeomLibrary.AsGML.column(n, Node(maxDigits), Node(options)))
      }
    def asKML[R](maxDigits: Column[Int] = ConstColumn(15), version: Option[Int] = None,  nPrefix: Option[String] = None)(implicit om: o#to[String, R]) =
      (version, nPrefix) match {
        case (Some(ver), Some(prefix)) => om(GeomLibrary.AsKML.column(LiteralNode(ver), n, Node(maxDigits), LiteralNode(prefix)))
        case (Some(ver), None) => om(GeomLibrary.AsKML.column(LiteralNode(ver), n, Node(maxDigits)))
        case (_, _)   => om(GeomLibrary.AsKML.column(n, Node(maxDigits)))
      }
    def asSVG[R](rel: Column[Int] = ConstColumn(0), maxDigits: Column[Int] = ConstColumn(15))(implicit om: o#to[String, R]) = {
        om(GeomLibrary.AsSVG.column(n, Node(rel), Node(maxDigits)))
      }
    def asX3D[R](maxDigits: Column[Int] = ConstColumn(15), options: Column[Int] = ConstColumn(0))(implicit om: o#to[String, R]) = {
        om(GeomLibrary.AsX3D.column(n, Node(maxDigits), Node(options)))
      }

    /** Spatial Relationships */
    def gEquals[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.Equals.column(n, Node(geom)))
      }
    def orderingEquals[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.OrderingEquals.column(n, Node(geom)))
      }
    def overlaps[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.Overlaps.column(n, Node(geom)))
      }
    def intersects[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.Intersects.column(n, Node(geom)))
      }
    def crosses[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.Crosses.column(n, Node(geom)))
      }
    def disjoint[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.Disjoint.column(n, Node(geom)))
      }
    def contains[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.Contains.column(n, Node(geom)))
      }
    def containsProperly[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.ContainsProperly.column(n, Node(geom)))
      }
    def within[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.Within.column(n, Node(geom)))
      }
    def dWithin[P2, R](geom: Column[P2], distance: Column[Double])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.DWithin.column(n, Node(geom), Node(distance)))
      }
    def dFullyWithin[P2, R](geom: Column[P2], distance: Column[Double])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.DFullyWithin.column(n, Node(geom), Node(distance)))
      }
    def touches[P2, R](geom: Column[P2])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.Touches.column(n, Node(geom)))
      }
    def relate[P2, R](geom: Column[P2], matrixPattern: Column[String])(implicit om: o#to[Boolean, R]) = {
        om(GeomLibrary.Relate.column(n, Node(geom), Node(matrixPattern)))
      }
    def relatePattern[P2, R](geom: Column[P2], boundaryNodeRule: Option[Int] = None)(implicit om: o#to[String, R]) =
      boundaryNodeRule match {
        case Some(rule) => om(GeomLibrary.Relate.column(n, Node(geom), LiteralNode(rule)))
        case None    => om(GeomLibrary.Relate.column(n, Node(geom)))
      }

    /** Spatial Measurements */
    def azimuth[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om(GeomLibrary.Azimuth.column(n, Node(geom)))
      }
    def centroid[R](implicit om: o#to[POINT, R]) = {
        om(GeomLibrary.Centroid.column[POINT](n))
      }
    def closestPoint[P2, R](geom: Column[P2])(implicit om: o#to[POINT, R]) = {
        om(GeomLibrary.ClosestPoint.column[POINT](n, Node(geom)))
      }
    def pointOnSurface[R](implicit om: o#to[POINT, R]) = {
        om(GeomLibrary.PointOnSurface.column[POINT](n))
      }
    def project[R](distance: Column[Float], azimuth: Column[Float])(implicit om: o#to[POINT, R]) = {
        om(GeomLibrary.Project.column[POINT](n, Node(distance), Node(azimuth)))
      }
    def length[R](implicit om: o#to[Float, R]) = {
        om(GeomLibrary.Length.column[Float](n))
      }
    def length3d[R](implicit om: o#to[Float, R]) = {
        om(GeomLibrary.Length3D.column[Float](n))
      }
    def perimeter[R](implicit om: o#to[Float, R]) = {
        om(GeomLibrary.Perimeter.column[Float](n))
      }
    def distance[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om(GeomLibrary.Distance.column(n, Node(geom)))
      }
    def distanceSphere[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om(GeomLibrary.DistanceSphere.column(n, Node(geom)))
      }
    def maxDistance[P2, R](geom: Column[P2])(implicit om: o#to[Float, R]) = {
        om(GeomLibrary.MaxDistance.column(n, Node(geom)))
      }
    def hausdorffDistance[P2, R](geom: Column[P2], densifyFrac: Option[Float] = None)(implicit om: o#to[Float, R]) =
      densifyFrac match {
        case Some(denFrac) => om(GeomLibrary.HausdorffDistance.column(n, Node(geom), LiteralNode(denFrac)))
        case None   => om(GeomLibrary.HausdorffDistance.column(n, Node(geom)))
      }
    def longestLine[P2, R](geom: Column[P2])(implicit om: o#to[LINESTRING, R]) = {
        om(GeomLibrary.LongestLine.column[LINESTRING](n, Node(geom)))
      }
    def shortestLine[P2, R](geom: Column[P2])(implicit om: o#to[LINESTRING, R]) = {
        om(GeomLibrary.ShortestLine.column[LINESTRING](n, Node(geom)))
      }

    /** Geometry Processing */
    def setSRID[R](srid: Column[Int])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.SetSRID.column[GEOMETRY](n, Node(srid)))
      }
    def transform[R](srid: Column[Int])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.Transform.column[GEOMETRY](n, Node(srid)))
      }
    def simplify[R](tolerance: Column[Float])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.Simplify.column[GEOMETRY](n, Node(tolerance)))
      }
    def removeRepeatedPoints[R](implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.RemoveRepeatedPoints.column[GEOMETRY](n))
      }
    def simplifyPreserveTopology[R](tolerance: Column[Float])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.SimplifyPreserveTopology.column[GEOMETRY](n, Node(tolerance)))
      }
    def difference[P2, R](geom: Column[P2])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.Difference.column[GEOMETRY](n, Node(geom)))
      }
    def symDifference[P2, R](geom: Column[P2])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.SymDifference.column[GEOMETRY](n, Node(geom)))
      }
    def intersection[P2, R](geom: Column[P2])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.Intersection.column[GEOMETRY](n, Node(geom)))
      }
    def sharedPaths[P2, R](geom: Column[P2])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.SharedPaths.column[GEOMETRY](n, Node(geom)))
      }
    def split[P2, R](blade: Column[P2])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.Split.column[GEOMETRY](n, Node(blade)))
      }
    def minBoundingCircle[R](segNumPerQtrCircle: Column[Int] = ConstColumn(48))(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.MinBoundingCircle.column[GEOMETRY](n, Node(segNumPerQtrCircle)))
      }

    def buffer[R](radius: Column[Float], bufferStyles: Option[String] = None)(implicit om: o#to[GEOMETRY, R]) =
      bufferStyles match {
        case Some(styles) => om(GeomLibrary.Buffer.column[GEOMETRY](n, Node(radius), Node(styles)))
        case None   =>  om(GeomLibrary.Buffer.column[GEOMETRY](n, Node(radius)))
      }
    def multi[R](implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.Multi.column[GEOMETRY](n))
      }
    def lineMerge[R](implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.LineMerge.column[GEOMETRY](n))
      }
    def collectionExtract[R](tpe: Column[Int])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.CollectionExtract.column[GEOMETRY](n, Node(tpe)))
      }
    def collectionHomogenize[R](implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.CollectionHomogenize.column[GEOMETRY](n))
      }
    def addPoint[P2, R](point: Column[P2], position: Option[Int] = None)(implicit om: o#to[GEOMETRY, R]) =
      position match {
        case Some(pos) => om(GeomLibrary.AddPoint.column[GEOMETRY](n, Node(point), Node(pos)))
        case None   =>  om(GeomLibrary.AddPoint.column[GEOMETRY](n, Node(point)))
      }
    def setPoint[P2, R](point: Column[P2], position: Column[Int])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.SetPoint.column[GEOMETRY](n, Node(position), Node(point)))
      }
    def removePoint[R](offset: Column[Int])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.RemovePoint.column[GEOMETRY](n, Node(offset)))
      }
    def reverse[R](implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.Reverse.column[GEOMETRY](n))
      }
    def scale[R](xFactor: Column[Float], yFactor: Column[Float], zFactor: Option[Float] = None)(implicit om: o#to[GEOMETRY, R]) =
      zFactor match {
        case Some(zFac) => om(GeomLibrary.Scale.column[GEOMETRY](n, Node(xFactor), Node(yFactor), LiteralNode(zFac)))
        case None   =>  om(GeomLibrary.Scale.column[GEOMETRY](n, Node(xFactor), Node(yFactor)))
      }
    def segmentize[R](maxLength: Column[Float])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.Segmentize.column[GEOMETRY](n, Node(maxLength)))
      }
    def snap[P2, R](reference: Column[P2], tolerance: Column[Float])(implicit om: o#to[GEOMETRY, R]) = {
        om(GeomLibrary.Snap.column[GEOMETRY](n, Node(reference), Node(tolerance)))
      }
    def translate[R](deltaX: Column[Float], deltaY: Column[Float], deltaZ: Option[Float] = None)(implicit om: o#to[GEOMETRY, R]) =
      deltaZ match {
        case Some(deltaZ) => om(GeomLibrary.Translate.column[GEOMETRY](n, Node(deltaX), Node(deltaY), LiteralNode(deltaZ)))
        case None   =>  om(GeomLibrary.Translate.column[GEOMETRY](n, Node(deltaX), Node(deltaY)))
      }
  }
}
