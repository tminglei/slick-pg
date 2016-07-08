package com.github.tminglei.slickpg

import java.util.concurrent.Executors

import com.vividsolutions.jts.geom.{Geometry, Point}
import com.vividsolutions.jts.io.{WKBWriter, WKTReader, WKTWriter}
import org.scalatest.FunSuite
import slick.jdbc.{GetResult, PostgresProfile}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class PgPostGISSupportSuite extends FunSuite {
  implicit val testExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))

  object MyPostgresProfile extends PostgresProfile with PgPostGISSupport {

    override val api = new API with PostGISImplicits with PostGISAssistants

    ///
    val plainAPI = new API with PostGISPlainImplicits
  }

  ///
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  case class GeometryBean(id: Long, geom: Geometry)

  class GeomTestTable(tag: Tag) extends Table[GeometryBean](tag, "geom_test") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def geom = column[Geometry]("geom", O.Default(new WKTReader().read("POINT(-71.064544 42.28787)")))

    def * = (id, geom) <> (GeometryBean.tupled, GeometryBean.unapply)
  }
  val GeomTests = TableQuery[GeomTestTable]

  ///
  case class PointBean(id: Long, point: Point)

  class PointTestTable(tag: Tag) extends Table[PointBean](tag, "point_test") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def point = column[Point]("point")

    def * = (id, point) <> (PointBean.tupled, PointBean.unapply)
  }
  val PointTests = TableQuery[PointTestTable]

  ////////////////////////////////////////////////////////////////////////////////

  val wktReader = new WKTReader()
  val wktWriter = new WKTWriter()
  val wkbWriter = new WKBWriter(2, true)

  test("PostGIS Lifted support - constructor") {
    val POINT = "POINT(-71.064544 42.28787)"
    val point = wktReader.read(POINT).asInstanceOf[Point]
    val point1 = wktReader.read("POINT(-81.064544 32.28787)").asInstanceOf[Point]
    val point2 = wktReader.read("POINT(-61.064544 52.28787)").asInstanceOf[Point]
    val line = wktReader.read("LINESTRING(-10 0, 50 50, -100 -100, 10 -70, -10 0)")

    val bean = GeometryBean(101L, point)

    Await.result(db.run(
      DBIO.seq(
        (GeomTests.schema ++ PointTests.schema) create,
        GeomTests.forceInsert(bean)
      ).andThen(
        DBIO.seq(
          GeomTests.filter(_.id === bean.id.bind).result.head.map(
            r => assert(bean === r)
          ),
          // geom_from_text
          GeomTests.filter(_.geom === geomFromText(POINT.bind)).result.head.map(
            r => assert(bean === r)
          ),
          // geom_from_wkb
          GeomTests.filter(_.geom === geomFromWKB(wkbWriter.write(point).bind)).result.head.map(
            r => assert(bean === r)
          ),
          // geom_from_wkt
          GeomTests.filter(_.geom === geomFromEWKT("SRID=0;POINT(-71.064544 42.28787)".bind)).result.head.map(
            r => assert(bean === r)
          ),
          // geom_from_gml
          GeomTests.filter(_.geom === geomFromGML("""<gml:Point xmlns:gml="http://www.opengis.net/gml">
                                                              <gml:coordinates>
                                                                -71.064544,42.28787
                                                              </gml:coordinates>
                                                             </gml:Point>""".bind)).result.head.map(
            r => assert(bean === r)
          ),
          // geom_from_kml
          GeomTests.filter(_.geom === geomFromKML("""<Point>
                                                            <coordinates>-71.064544,42.28787</coordinates>
                                                       </Point>""".bind).setSRID(0.bind)).result.head.map(
            r => assert(bean === r)
          ),
          // disable it, since JSON-C not sure enabled
  //      Await.result(db.run(DBIO.seq(
  //        Tests.filter(_.geom === geomFromGeoJSON("""{"type":"Point","coordinates":[-71.064544,42.28787]}""".bind)).result.head.map(
  //          r => assert(bean === r)
  //        )
  //      )), Duration.Inf)
          // make_box
          GeomTests.filter(_.geom @&& makeBox(point1.bind, point2.bind)).result.head.map(
            r => assert(bean === r)
          ),
          // make_box_3d
          GeomTests.filter(_.geom @&& makeBox3d(point1.bind, point2.bind)).result.head.map(
            r => assert(bean === r)
          ),
          // make_envelope
          GeomTests.filter(_.geom @&& makeEnvelope(-61.064544.bind, 32.28787.bind, -81.064544.bind, 52.28787.bind)).result.head.map(
            r => assert(bean === r)
          ),
          // make_point
          GeomTests.filter(_.geom @&& makeEnvelope(-61.064544.bind, 32.28787.bind, -81.064544.bind, 52.28787.bind)).result.head.map(
            r => assert(bean === r)
          ),
          // make_line
          GeomTests.filter(_.geom @&& makeLine(point1.bind, point2.bind)).result.head.map(
            r => assert(bean === r)
          ),
          // make_polygon
          GeomTests.filter(_.geom @&& makePolygon(line.bind)).result.head.map(
            r => assert(bean === r)
          )
        )
      ).andFinally(
        (GeomTests.schema ++ PointTests.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("PostGIS Lifted support - operator") {
    val line1 = wktReader.read("LINESTRING(0 0, 3 3)")
    val line2 = wktReader.read("LINESTRING(1 2, 4 6)")
    val line3 = wktReader.read("LINESTRING(1 1, 2 2)")
    val point = wktReader.read("POINT(4 5)").asInstanceOf[Point]
    val point1 = wktReader.read("POINT(7 9)").asInstanceOf[Point]
    val point2 = wktReader.read("POINT(11 13)").asInstanceOf[Point]
    val line3d1 = wktReader.read("LINESTRING(0 0 1, 3 3 2)")
    val line3d2 = wktReader.read("LINESTRING(1 2 1, 4 6 1)")

    val bean = GeometryBean(111L, line1)
    val bean3d = GeometryBean(112L, line3d1)
    val pbean1 = PointBean(121L, point1)
    val pbean2 = PointBean(122L, point2)

    Await.result(db.run(
      DBIO.seq(
        (GeomTests.schema ++ PointTests.schema) create,
        GeomTests forceInsertAll List(bean, bean3d),
        PointTests forceInsertAll List(pbean1, pbean2)
      ).andThen(
        DBIO.seq(
          // &&
          GeomTests.filter(r => { r.id === bean.id.bind && r.geom @&& line2.bind }).result.head.map(
            r => assert(bean === r)
          ),
          // ~
          GeomTests.filter(r => { r.id === bean.id.bind && r.geom @> line3.bind }).result.head.map(
            r => assert(bean === r)
          ),
          // @
          GeomTests.filter(r => { r.id === bean.id.bind && line3.bind <@ r.geom }).result.head.map(
            r => assert(bean === r)
          ),
          // <->
          GeomTests.filter(r => { r.id === bean.id.bind && (r.geom <-> line2) > 0.7d.bind }).result.head.map(
            r => assert(bean === r)
          ),
          PointTests.sortBy(r => r.point <-> point.bind).to[List].result.map(
            r => assert(List(pbean1, pbean2) === r)
          ),
          PointTests.sortBy(r => r.point <-> Option(point).bind).to[List].result.map(
            r => assert(List(pbean1, pbean2) === r)
          ),
          // <#>
          GeomTests.filter(r => { r.id === bean.id.bind && (r.geom <#> line2.bind) === 0.0d.bind}).result.head.map(
            r => assert(bean === r)
          ),
          // &<
          GeomTests.filter(r => { r.id === bean.id.bind && (r.geom &< line2.bind) }).result.head.map(
            r => assert(bean === r)
          ),
          // &>
          GeomTests.filter(r => { r.id === bean.id.bind && (line2.bind &> r.geom) }).result.head.map(
            r => assert(bean === r)
          ),
          // <<
          GeomTests.filter(r => { r.id === bean.id.bind && (r.geom << point.bind) }).result.head.map(
            r => assert(bean === r)
          ),
          // >>
          GeomTests.filter(r => { r.id === bean.id.bind && (point.bind >> r.geom) }).result.head.map(
            r => assert(bean === r)
          ),
          // &<|
          GeomTests.filter(r => { r.id === bean.id.bind && (r.geom &<| line2.bind) }).result.head.map(
            r => assert(bean === r)
          ),
          // |&>
          GeomTests.filter(r => { r.id === bean.id.bind && (line2.bind |&> r.geom) }).result.head.map(
            r => assert(bean === r)
          ),
          // <<|
          GeomTests.filter(r => { r.id === bean.id.bind && (r.geom <<| point.bind) }).result.head.map(
            r => assert(bean === r)
          ),
          // |>>
          GeomTests.filter(r => { r.id === bean.id.bind && (point.bind |>> r.geom) }).result.head.map(
            r => assert(bean === r)
          )
        ).flatMap { v =>
          val latLongPoint = makePoint(point2.getX, point2.getY).setSRID(4326)
          val distanceQuery = PointTests.filter(_.id === pbean1.id.bind)

          distanceQuery.map(_.point.setSRID(4326).distanceSphere(latLongPoint)).result.head.flatMap { d =>
            distanceQuery.map { p =>
              (p.point.setSRID(4326).dWithin(latLongPoint, d * 1.01, Some(true)),
                p.point.setSRID(4326).dWithin(latLongPoint, d * 0.99, Some(true)))
            }.result.head.map(
              r => assert((true, false) === r)
            )
          }
        }
      ).andFinally(
        (GeomTests.schema ++ PointTests.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("PostGIS Lifted support - accessor") {
    val point = wktReader.read("POINT(4 5 7)")
    val line = wktReader.read("LINESTRING(0 0, 3 3)")
    val polygon = wktReader.read("POLYGON((0 0, 1 1, 1 2, 1 1, 0 0))")
    val collection = wktReader.read("GEOMETRYCOLLECTION(LINESTRING(1 1,0 0),POINT(0 0))")

    val pointbean = GeometryBean(130L, point)
    val linebean = GeometryBean(131L, line)
    val polygonbean = GeometryBean(132L, polygon)
    val collectionbean = GeometryBean(133L, collection)

    Await.result(db.run(
      DBIO.seq(
        (GeomTests.schema ++ PointTests.schema) create,
        GeomTests forceInsertAll List(pointbean, linebean, polygonbean, collectionbean)
      ).andThen(
        DBIO.seq(
          // goem_type
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.geomType).result.head.map(
            r => assert("ST_LineString" === r)
          ),
          // srid
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.srid).result.head.map(
            r => assert(0 === r)
          ),
          // is_valid
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.isValid).result.head.map(
            r => assert(false === r)
          ),
          // is_closed
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.isClosed).result.head.map(
            r => assert(true === r)
          ),
          // is_collection
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.isCollection).result.head.map(
            r => assert(false === r)
          ),
          // is_empty
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.isEmpty).result.head.map(
            r => assert(false === r)
          ),
          // is_ring
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.isRing).result.head.map(
            r => assert(false === r)
          ),
          // is_simple
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.isSimple).result.head.map(
            r => assert(true === r)
          ),
          // has_arc
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.hasArc).result.head.map(
            r => assert(false === r)
          ),
          // area
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.area).result.head.map(
            r => assert(Math.abs(0.0f - r) < 0.001f)
          ),
          // boundary
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.boundary).result.head.map(
            r => assert(wktReader.read("MULTIPOINT(0 0, 3 3)") === r)
          ),
          // dimension
          GeomTests.filter(_.id === collectionbean.id.bind).map(_.geom.dimension).result.head.map(
            r => assert(1 === r)
          ),
          // coord_dim
          GeomTests.filter(_.id === collectionbean.id.bind).map(_.geom.coordDim).result.head.map(
            r => assert(2 === r)
          ),
          // nDims
          GeomTests.filter(_.id === collectionbean.id.bind).map(_.geom.nDims).result.head.map(
            r => assert(2 === r)
          ),
          // nPoints
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.nPoints).result.head.map(
            r => assert(2 === r)
          ),
          // nRings
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.nRings).result.head.map(
            r => assert(1 === r)
          ),
          // x
          GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.x).result.head.map(
            r => assert(Math.abs(4f - r) < 0.001f)
          ),
          // y
          GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.y).result.head.map(
            r => assert(Math.abs(5f - r) < 0.001f)
          ),
          // z
          GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.z.?).result.head.map(
            r => {
              assert(r.isDefined)
              r.map(v => assert(Math.abs(7f - v) < 0.001f))
            }
          ),
          // xmin
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.xmin).result.head.map(
            r => assert(Math.abs(0f - r) < 0.001f)
          ),
          // xmax
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.xmax).result.head.map(
            r => assert(Math.abs(1f - r) < 0.001f)
          ),
          // ymin
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.ymin).result.head.map(
            r => assert(Math.abs(0f - r) < 0.001f)
          ),
          // ymax
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.ymax).result.head.map(
            r => assert(Math.abs(2f - r) < 0.001f)
          ),
          // zmin
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.zmin.?).result.head.map(
            r => {
              assert(r.isDefined)
              r.map(v => assert(Math.abs(0.0f - v) < 0.001f))
            }
          ),
          // zmax
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.zmax.?).result.head.map(
            r => {
              assert(r.isDefined)
              r.map(v => assert(Math.abs(0.0 - v) < 0.001f))
            }
          )
        )
      ).andFinally(
        (GeomTests.schema ++ PointTests.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("PostGIS Lifted support - output") {
    val POLYGON = "POLYGON((0 0,0 1,1 1,1 0,0 0))"
    val polygon = wktReader.read(POLYGON)
    val POLYGON_EWKT = POLYGON // not "SRID=0;POLYGON((0 0,0 1,1 1,1 0,0 0))"
    val POLYGON_HEX = "01030000000100000005000000000000000000000000000000000000000000000000000000000000000000F03F000000000000F03F000000000000F03F000000000000F03F000000000000000000000000000000000000000000000000"
    val POLYGON_GML = """<gml:Polygon><gml:outerBoundaryIs><gml:LinearRing><gml:coordinates>0,0 0,1 1,1 1,0 0,0</gml:coordinates></gml:LinearRing></gml:outerBoundaryIs></gml:Polygon>"""
    val POLYGON_KML = """<Polygon><outerBoundaryIs><LinearRing><coordinates>0,0 0,1 1,1 1,0 0,0</coordinates></LinearRing></outerBoundaryIs></Polygon>"""
    val POLYGON_JSON = """{"type":"Polygon","coordinates":[[[0,0],[0,1],[1,1],[1,0],[0,0]]]}"""
    val POLYGON_SVG = """M 0 0 L 0 -1 1 -1 1 0 Z"""

    val POINT = "POINT(-3.2342342 -2.32498)"
    val point = wktReader.read(POINT)
    val POINT_LatLon = """2°19'29.928"S 3°14'3.243"W"""

    val bean = GeometryBean(141L, polygon)
    val bean1 = GeometryBean(142L, point)

    Await.result(db.run(
      DBIO.seq(
        (GeomTests.schema ++ PointTests.schema) create,
        GeomTests forceInsertAll List(bean, bean1)
      ).andThen(
        DBIO.seq(
          // as_text
          GeomTests.filter(_.id === bean.id.bind).map(_.geom.asText).result.head.map(
            r => assert(POLYGON === r)
          ),
          // as_EWKT
          GeomTests.filter(_.id === bean.id.bind).map(_.geom.asEWKT).result.head.map(
            r => assert(POLYGON_EWKT === r)
          ),
          // as_Hex_EWKB
          GeomTests.filter(_.id === bean.id.bind).map(_.geom.asHEXEWKB()).result.head.map(
            r => assert(POLYGON_HEX === r)
          ),
          // as_lat_lon_text
          GeomTests.filter(_.id === bean1.id.bind).map(_.geom.asLatLonText()).result.head.map(
            r => assert(POINT_LatLon === r)
          ),
          // as_GML
          GeomTests.filter(_.id === bean.id.bind).map(_.geom.asGML()).result.head.map(
            r => assert(POLYGON_GML === r)
          ),
          // as_KML
          GeomTests.filter(_.id === bean.id.bind).map(_.geom.setSRID(4326).asKML()).result.head.map(
            r => assert(POLYGON_KML === r)
          ),
          // as_GeoJson
          GeomTests.filter(_.id === bean.id.bind).map(_.geom.asGeoJSON()).result.head.map(
            r => assert(POLYGON_JSON === r)
          ),
          // as_SVG
          GeomTests.filter(_.id === bean.id.bind).map(_.geom.asSVG()).result.head.map(
            r => assert(POLYGON_SVG === r)
          )
        )
      ).andFinally(
        (GeomTests.schema ++ PointTests.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("PostGIS Lifted support - relationship") {
    val polygon = wktReader.read("POLYGON((175 150, 20 40, 50 60, 125 100, 175 150))")
    val multiPoints = wktReader.read("MULTIPOINT(125 100, 125 101)")
    val point = wktReader.read("POINT(175 150)")

    val line1 = wktReader.read("LINESTRING(0 0, 100 100)")
    val line2 = wktReader.read("LINESTRING(0 0, 5 5, 100 100)")

    val linebean = GeometryBean(151L, line1)
    val polygonbean = GeometryBean(152L, polygon)

    Await.result(db.run(
      DBIO.seq(
        (GeomTests.schema ++ PointTests.schema) create,
        GeomTests forceInsertAll List(linebean, polygonbean)
      ).andThen(
        DBIO.seq(
          // equals
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.gEquals(line2.bind)).result.head.map(
            r => assert(true === r)
          ),
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.gEquals(line2.bind.reverse)).result.head.map(
            r => assert(true === r)
          ),
          // ordering_equals
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.orderingEquals(line2.bind)).result.head.map(
            r => assert(false === r)
          ),
          // overlaps
          GeomTests.filter(_.id === polygonbean.id.bind).map(
            _.geom.overlaps(geomFromText("LINESTRING(0 0, 10 10)".bind).buffer(10f.bind))).result.head.map(
              r => assert(false === r)
            ),
          // intersects
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.intersects(line1.bind)).result.head.map(
            r => assert(true === r)
          ),
          // crosses
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.crosses(line1.bind)).result.head.map(
            r => assert(true === r)
          ),
          // disjoint
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.disjoint(line1.bind)).result.head.map(
            r => assert(false === r)
          ),
          // contains
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.contains(multiPoints.bind)).result.head.map(
            r => assert(true === r)
          ),
          // contains_properly
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.containsProperly(multiPoints.bind)).result.head.map(
            r => assert(false === r)
          ),
          // within
          GeomTests.filter(_.id === polygonbean.id.bind).map(r => multiPoints.bind.within(r.geom)).result.head.map(
            r => assert(true === r)
          ),
          // dWithin
          GeomTests.filter(_.id === polygonbean.id.bind).map(r => multiPoints.bind.dWithin(r.geom, 10d.bind)).result.head.map(
            r => assert(true === r)
          ),
          // dFullyWithin
          GeomTests.filter(_.id === polygonbean.id.bind).map(r => point.bind.dFullyWithin(r.geom, 200d.bind)).result.head.map(
            r => assert(true === r)
          ),
          // touches
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.touches(point.bind)).result.head.map(
            r => assert(true === r)
          ),
          // relate
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.relate(multiPoints.bind, "T*****FF*".bind)).result.head.map(
            r => assert(true === r)
          ),
          // relate_pattern
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.relatePattern(point.bind)).result.head.map(
            r => assert("FF20F1FF2" === r)
          )
        )
      ).andFinally(
        (GeomTests.schema ++ PointTests.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("PostGIS Lifted support - measure") {
    val point1 = wktReader.read("POINT(25 45)").asInstanceOf[Point]
    val point2 = wktReader.read("POINT(75 100)").asInstanceOf[Point]
    val point3 = wktReader.read("POINT(-75 80)").asInstanceOf[Point]

    val line = wktReader.read("LINESTRING(743238 2967416,743238 2967450,743265 2967450,743265.625 2967416,743238 2967416)")
    val line3d = wktReader.read("LINESTRING(743238 2967416 1,743238 2967450 1,743265 2967450 3,743265.625 2967416 3,743238 2967416 3)")

    val polygon = wktReader.read("POLYGON((175 150, 20 40, 50 60, 125 100, 175 150))")
    val centroid = wktReader.read("POINT(113.07692307692308 101.28205128205128)")
    val closetPoint = wktReader.read("POINT(84.89619377162629 86.0553633217993)")
    val projectedPoint = wktReader.read("POINT(25.008969098766023 45.006362421852465)")
    val longestLine = wktReader.read("LINESTRING(175 150, 75 100)")
    val shortestLine = wktReader.read("LINESTRING(84.89619377162629 86.0553633217993, 75 100)")

    val pointbean = GeometryBean(161L, point1)
    val linebean = GeometryBean(162L, line)
    val line3dbean = GeometryBean(163L, line3d)
    val polygonbean = GeometryBean(164L, polygon)
    val pbean1 = PointBean(166L, point1)
    val pbean2 = PointBean(167L, point2)

    Await.result(db.run(
      DBIO.seq(
        (GeomTests.schema ++ PointTests.schema) create,
        GeomTests forceInsertAll List(pointbean, linebean, line3dbean, polygonbean),
        PointTests forceInsertAll List(pbean1, pbean2)
      ).andThen(
        DBIO.seq(
          // azimuth
          GeomTests.filter(_.id === pointbean.id.bind).map((_.geom.azimuth(point2.bind).toDegrees)).result.head.map(
            r => assert(Math.abs(42.2736890060937d - r) < 0.1d)
          ),
          // centroid
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.centroid).result.head.map(
            r => assert(centroid === r)
          ),
          GeomTests.filter(_.id === polygonbean.id.bind).map(r => (r.geom.centroid within r.geom)).result.head.map(
            r => assert(true === r)
          ),
          // closest_point
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.closestPoint(point2.bind)).result.head.map(
            r => assert(closetPoint === r)
          ),
          // point_on_surface
          GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.pointOnSurface).result.head.map(
            r => assert(point1 === r)
          ),
          // project
          GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.project(1000f.bind, 45f.bind.toRadians)).result.head.map(
            r => assert(projectedPoint === r)
          ),
          // length
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.length).result.head.map(
            r => assert(Math.abs(122.630744000095d - r) < 0.1d)
          ),
          // length_3d
          GeomTests.filter(_.id === line3dbean.id.bind).map(_.geom.length3d).result.head.map(
            r => assert(Math.abs(122.704716741457d - r) < 0.1d)
          ),
          // perimeter
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.perimeter).result.head.map(
            r => assert(Math.abs(381.83197021484375d - r) < 0.1d)
          ),
          // distance
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.distance(point2.bind)).result.head.map(
            r => assert(Math.abs(17.09934425354004d - r) < 0.1d)
          ),
          PointTests.sortBy(_.point.distance(point3.bind)).to[List].result.map(
            r => assert(List(pbean1, pbean2) === r)
          ),
          // distance_sphere
          GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.distanceSphere(point3.bind)).result.head.map(
            r => assert(Math.abs(5286499.0d - r) < 0.1d)
          ),
          // max_distance
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.maxDistance(point2.bind)).result.head.map(
            r => assert(Math.abs(111.80339813232422d - r) < 0.1d)
          ),
          // hausdorff_distance
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.hausdorffDistance(line3d.bind)).result.head.map(
            r => assert(Math.abs(0.0d - r) < 0.1d)
          ),
          // longest_line
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.longestLine(point2.bind)).result.head.map(
            r => assert(longestLine === r)
          ),
          // shortest_line
          GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.shortestLine(point2.bind)).result.head.map(
            r => assert(shortestLine === r)
          )
        )
      ).andFinally(
        (GeomTests.schema ++ PointTests.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("PostGIS Lifted support - processing") {
    val point = wktReader.read("POINT(-123.365556 48.428611)")
    val point1 = wktReader.read("POINT(3 1)")
    val line = wktReader.read("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1)")
    val line1 = wktReader.read("LINESTRING(1 2,2 1,3 1)")
    val bladeLine = wktReader.read("LINESTRING(0 4,4 0)")
    val multiLine = wktReader.read("MULTILINESTRING((-29 -27,-30 -29.7,-36 -31,-45 -33),(-45 -33,-46 -32))")
    val collection = wktReader.read("GEOMETRYCOLLECTION(GEOMETRYCOLLECTION(LINESTRING(0 0, 1 1)),LINESTRING(2 2, 3 3))")

    point.setSRID(4326)

    val pointbean = GeometryBean(171L, point)
    val linebean = GeometryBean(172L, line)
    val multilinebean = GeometryBean(173L, multiLine)
    val collectionbean = GeometryBean(174L, collection)

    Await.result(db.run(
      DBIO.seq(
        (GeomTests.schema ++ PointTests.schema) create,
        GeomTests forceInsertAll List(pointbean, linebean, multilinebean, collectionbean)
      ).andThen(
        DBIO.seq(
          // set_SRID
          GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.setSRID(0.bind).asEWKT).result.head.map(
            r => assert("POINT(-123.365556 48.428611)" === r)
          ),
          // transform
          GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.transform(26986.bind).asEWKT).result.head.map(
            r => assert("SRID=26986;POINT(-3428094.64636768 2715245.01412978)" === r)
          ),
          // simplify
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.simplify(0.5f.bind).asText).result.head.map(
            r => assert("LINESTRING(1 1,2 2,2 3.5,1 3,2 1)" === r)
          ),
          // simplify_preserve_topology
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.simplifyPreserveTopology(0.5f.bind).asText).result.head.map(
            r => assert("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1)" === r)
          ),
          // remove_repeated_points
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.removeRepeatedPoints.asText).result.head.map(
            r => assert("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1)" === r)
          ),
          // difference
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.difference(line1.bind).asText).result.head.map(
            r => assert("MULTILINESTRING((1 1,1.5 1.5),(1.5 1.5,2 2,2 3.5,1 3,1 2))" === r)
          ),
          // sym_difference
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.symDifference(line1.bind).asText).result.head.map(
            r => assert("MULTILINESTRING((1 1,1.5 1.5),(1.5 1.5,2 2,2 3.5,1 3,1 2),(2 1,3 1))" === r)
          ),
          // intersection
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.intersection(line1.bind).asText).result.head.map(
            r => assert("MULTILINESTRING((1 2,1.5 1.5),(1.5 1.5,2 1))" === r)
          ),
          // shared_paths
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.sharedPaths(line1.bind).asText).result.head.map(
            r => assert("GEOMETRYCOLLECTION(MULTILINESTRING((1 2,1.5 1.5),(1.5 1.5,2 1)),MULTILINESTRING EMPTY)" === r)
          ),
          // split
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.split(bladeLine.bind).asText).result.head.map(
            r => assert("GEOMETRYCOLLECTION(LINESTRING(1 1,1.5 1.5),LINESTRING(1.5 1.5,2 2),LINESTRING(2 2,2 3.5,1 3),LINESTRING(1 3,1 2,1.5 1.5),LINESTRING(1.5 1.5,2 1))" === r)
          ),
          // min_bounding_circle
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.minBoundingCircle(8.bind).asText).result.head.map(
            r => assert("POLYGON((2.84629120178363 2.25,2.82042259384576 1.98735161591655,2.74381088612791 1.73479666193852,2.61940022359336 1.50204068331283,2.45197163823299 1.29802836176701,2.24795931668717 1.13059977640664,2.01520333806148 1.00618911387209,1.76264838408345 0.929577406154245,1.5 0.903708798216374,1.23735161591655 0.929577406154244,0.984796661938523 1.00618911387208,0.752040683312833 1.13059977640664,0.548028361767014 1.29802836176701,0.380599776406643 1.50204068331283,0.256189113872087 1.73479666193852,0.179577406154245 1.98735161591655,0.153708798216374 2.25,0.179577406154243 2.51264838408344,0.256189113872083 2.76520333806147,0.380599776406638 2.99795931668717,0.548028361767008 3.20197163823298,0.752040683312826 3.36940022359336,0.984796661938515 3.49381088612791,1.23735161591655 3.57042259384575,1.49999999999999 3.59629120178363,1.76264838408344 3.57042259384576,2.01520333806148 3.49381088612792,2.24795931668717 3.36940022359336,2.45197163823299 3.20197163823299,2.61940022359336 2.99795931668717,2.74381088612791 2.76520333806148,2.82042259384575 2.51264838408345,2.84629120178363 2.25))".replaceAll("[0-9],", ",").replaceAll("[0-9] ", " ") === r.replaceAll("[0-9],", ",").replaceAll("[0-9] ", " "))
          ),
          // buffer
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.buffer(10f.bind).asText).result.head.map(
            r => assert("POLYGON((-8.95075429832142 1.5,-9 2,-9 3,-8.83023553552631 4.83479408001984,-8.32670613678483 6.60729159315686,-7.5065080835204 8.25731112119134,-6.39748947238797 9.72882972781368,-5.03730469350959 10.9718850993806,-3.47213595499958 11.9442719099992,-2.47213595499958 12.4442719099992,-0.572769319290237 13.1633771544796,1.43271648611295 13.4838965046154,3.46160106847735 13.3926094796381,5.4301989043202 12.8932814009163,7.25731112119134 12.0065080835204,8.86757471241229 10.7688663194088,10.1945710395115 9.2314051923066,11.1835654057703 7.45754045310197,11.7937647015257 5.52043880658346,12 3.5,12 2,11.9507542983214 1.5,12 0.999999999999993,11.8078528040323 -0.950903220161287,11.2387953251129 -2.8268343236509,10.3146961230255 -4.55570233019602,9.07106781186548 -6.07106781186547,7.55570233019603 -7.31469612302545,5.82683432365091 -8.23879532511286,3.95090322016129 -8.8078528040323,2.00000000000001 -9,1.5 -8.95075429832142,0.999999999999994 -9,-0.950903220161286 -8.8078528040323,-2.8268343236509 -8.23879532511287,-4.55570233019602 -7.31469612302545,-6.07106781186547 -6.07106781186548,-7.31469612302545 -4.55570233019603,-8.23879532511286 -2.8268343236509,-8.8078528040323 -0.950903220161295,-9 0.99999999999999,-8.95075429832142 1.5))".replaceAll("[0-9],", ",").replaceAll("[0-9] ", " ") === r.replaceAll("[0-9],", ",").replaceAll("[0-9] ", " "))
          ),
          // multi
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.multi.asText).result.head.map(
            r => assert("MULTILINESTRING((1 1,2 2,2 3.5,1 3,1 2,2 1))" === r)
          ),
          // line_merge
          GeomTests.filter(_.id === multilinebean.id.bind).map(_.geom.lineMerge.asText).result.head.map(
            r => assert("LINESTRING(-29 -27,-30 -29.7,-36 -31,-45 -33,-46 -32)" === r)
          ),
          // collection_extract
          GeomTests.filter(_.id === collectionbean.id.bind).map(_.geom.collectionExtract(2.bind).asText).result.head.map(
            r => assert("MULTILINESTRING((0 0,1 1),(2 2,3 3))" === r)
          ),
          // collection_homogenize
          GeomTests.filter(_.id === collectionbean.id.bind).map(_.geom.collectionHomogenize).result.head.map(
            r => assert("MULTILINESTRING ((0 0, 0 1), (2 2, 3 3))" === wktWriter.write(r))
          ),
          // add_point
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.addPoint(point1.bind).asText).result.head.map(
            r => assert("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1,3 1)" === r)
          ),
          // set_point
          GeomTests.filter(_.id === linebean.id.bind).map(r => r.geom.setPoint(point1.bind, (r.geom.nPoints - 1.bind)).asText).result.head.map(
            r => assert("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,3 1)" === r)
          ),
          // remove_point
          GeomTests.filter(_.id === linebean.id.bind).map(r => r.geom.removePoint((r.geom.nPoints - 1.bind)).asText).result.head.map(
            r => assert("LINESTRING(1 1,2 2,2 3.5,1 3,1 2)" === r)
          ),
          // scale
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.scale(1f.bind, 2f.bind).asText).result.head.map(
            r => assert("LINESTRING(1 2,2 4,2 7,1 6,1 4,2 2)" === r)
          ),
          // segmentize
          GeomTests.filter(_.id === linebean.id.bind).map(_.geom.segmentize((5f.bind)).asText).result.head.map(
            r => assert("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1)" === r)
          ),
          // snap
          GeomTests.filter(_.id === linebean.id.bind).map(r => r.geom.snap(line1.bind, r.geom.distance(line1.bind)).asText).result.head.map(
            r => assert("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1)" === r)
          )
        )
      ).andFinally(
        (GeomTests.schema ++ PointTests.schema) drop
      ).transactionally
    ), Duration.Inf)
  }

  test("PostGIS Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val GetPointBeanResult = GetResult(r => PointBean(r.nextLong, r.nextGeometry[Point]))

    val b = PointBean(77L, wktReader.read("POINT(4 5)").asInstanceOf[Point])

    Await.result(db.run(
      DBIO.seq(
        sqlu"""create table point_test(
              id int8 not null primary key,
              point geometry not null)
          """,
        ///
        sqlu""" insert into point_test values(${b.id}, ${b.point}) """,
        sql""" select * from point_test where id = ${b.id} """.as[PointBean].head.map(
          r => assert(b === r)
        ),
        ///
        sqlu"drop table if exists point_test cascade"
      ).transactionally
    ), Duration.Inf)
  }
}