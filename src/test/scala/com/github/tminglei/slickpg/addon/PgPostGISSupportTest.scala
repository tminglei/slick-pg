package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import com.vividsolutions.jts.geom.{Geometry, Point}
import com.vividsolutions.jts.io.{WKTWriter, WKBWriter, WKTReader}
import scala.util.Try

class PgPostGISSupportTest {
  import scala.slick.driver.PostgresDriver

  object MyPostgresDriver extends PostgresDriver
                            with PgPostGISSupport {

    override lazy val Implicit = new Implicits with PostGISImplicits
    override val simple = new Implicits with SimpleQL with PostGISImplicits with PostGISAssistants
  }

  ///
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=postgres", driver = "org.postgresql.Driver")

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

  @Test
  def testGeomConstructors(): Unit = {
    val POINT = "POINT(-71.064544 42.28787)"
    val point = wktReader.read(POINT).asInstanceOf[Point]
    val point1 = wktReader.read("POINT(-81.064544 32.28787)").asInstanceOf[Point]
    val point2 = wktReader.read("POINT(-61.064544 52.28787)").asInstanceOf[Point]
    val line = wktReader.read("LINESTRING(-10 0, 50 50, -100 -100, 10 -70, -10 0)")

    db withSession { implicit session: Session =>
      val bean = GeometryBean(101L, point)
      GeomTests.forceInsert(bean)

      val q0 = GeomTests.filter(_.id === bean.id.bind).map(t => t)
      assertEquals(bean, q0.first)

      val q1 = GeomTests.filter(_.geom === geomFromText(POINT.bind)).map(t => t)
      assertEquals(bean, q1.first)

      val q2 = GeomTests.filter(_.geom === geomFromWKB(wkbWriter.write(point).bind)).map(t => t)
      assertEquals(bean, q2.first)

      val q3 = GeomTests.filter(_.geom === geomFromEWKT("SRID=0;POINT(-71.064544 42.28787)".bind)).map(t => t)
      assertEquals(bean, q3.first)

      val q4 = GeomTests.filter(_.geom === geomFromGML("""<gml:Point xmlns:gml="http://www.opengis.net/gml">
                                                              <gml:coordinates>
                                                                -71.064544,42.28787
                                                              </gml:coordinates>
                                                             </gml:Point>""".bind)).map(t => t)
      assertEquals(bean, q4.first)

      val q5 = GeomTests.filter(_.geom === geomFromKML("""<Point>
                                                            <coordinates>-71.064544,42.28787</coordinates>
                                                       </Point>""".bind).setSRID(0.bind)).map(t => t)
      assertEquals(bean, q5.first)

      // disable it, since JSON-C not enabled
//      val q6 = GeomTests.filter(_.geom === geomFromGeoJSON("""{"type":"Point","coordinates":[-71.064544,42.28787]}""".bind)).map(t => t)
//      assertEquals(bean, q6.first)

      val q7 = GeomTests.filter(_.geom @&& makeBox(point1.bind, point2.bind)).map(t => t)
      assertEquals(bean, q7.first)

      val q71 = GeomTests.filter(_.geom @&& makeBox3d(point1.bind, point2.bind)).map(t => t)
      assertEquals(bean, q71.first)
      
      val q72 = GeomTests.filter(_.geom @&& makeEnvelope(-61.064544.bind, 32.28787.bind, -81.064544.bind, 52.28787.bind)).map(t => t)
      assertEquals(bean, q72.first)

      val q8 = GeomTests.filter(_.geom === makePoint((-71.064544D).bind, (42.28787D).bind)).map(t => t)
      assertEquals(bean, q8.first)

      val q9 = GeomTests.filter(_.geom @&& makeLine(point1.bind, point2.bind)).map(t => t)
      assertEquals(bean, q9.first)

      val q10 = GeomTests.filter(_.geom @&& makePolygon(line.bind)).map(t => t)
      assertEquals(bean, q10.first)
    }
  }

  @Test
  def testGeomOperators(): Unit = {
    val line1 = wktReader.read("LINESTRING(0 0, 3 3)")
    val line2 = wktReader.read("LINESTRING(1 2, 4 6)")
    val line3 = wktReader.read("LINESTRING(1 1, 2 2)")
    val point = wktReader.read("POINT(4 5)").asInstanceOf[Point]
    val point1 = wktReader.read("POINT(7 9)").asInstanceOf[Point]
    val point2 = wktReader.read("POINT(11 13)").asInstanceOf[Point]
    val line3d1 = wktReader.read("LINESTRING(0 0 1, 3 3 2)")
    val line3d2 = wktReader.read("LINESTRING(1 2 1, 4 6 1)")

    db withSession { implicit session: Session =>
      val bean = GeometryBean(111L, line1)
      val bean3d = GeometryBean(112L, line3d1)
      GeomTests.forceInsertAll(bean, bean3d)

      //
      val pbean1 = PointBean(121L, point1)
      val pbean2 = PointBean(122L, point2)
      PointTests.forceInsertAll(pbean1, pbean2)

      ///
      val q1 = GeomTests.filter(r => { r.id === bean.id.bind && r.geom @&& line2.bind }).map(r => r)
      assertEquals(bean, q1.first)

      val q2 = GeomTests.filter(r => { r.id === bean3d.id.bind && r.geom @&&& line3d2.bind }).map(r => r)
      assertEquals(bean3d, q2.first)

      val q3 = GeomTests.filter(r => { r.id === bean.id.bind && r.geom @> line3.bind }).map(r => r)
      assertEquals(bean, q3.first)

      val q4 = GeomTests.filter(r => { r.id === bean.id.bind && line3.bind <@ r.geom }).map(r => r)
      assertEquals(bean, q4.first)

      val q5 = GeomTests.filter(r => { r.id === bean.id.bind && (r.geom <-> line2) > 0.7d.bind }).map(r => r)
      assertEquals(bean, q5.first)
      val q51 = PointTests.sortBy(r => r.point <-> point.bind).map(r => r)
      assertEquals(List(pbean1, pbean2), q51.list)
      val q52 = PointTests.sortBy(r => r.point <-> Option(point).bind).map(r => r)
      assertEquals(List(pbean1, pbean2), q52.list)

      val q6 = GeomTests.filter(r => { r.id === bean.id.bind && (r.geom <#> line2.bind) === 0.0d.bind}).map(r => r)
      assertEquals(bean, q6.first)

      ///
      val q7 = GeomTests.filter(r => { r.id === bean.id.bind && (r.geom &< line2.bind) }).map(r => r)
      assertEquals(bean, q7.first)

      val q8 = GeomTests.filter(r => { r.id === bean.id.bind && (line2.bind &> r.geom) }).map(r => r)
      assertEquals(bean, q8.first)

      val q9 = GeomTests.filter(r => { r.id === bean.id.bind && (r.geom << point.bind) }).map(r => r)
      assertEquals(bean, q9.first)

      val q10 = GeomTests.filter(r => { r.id === bean.id.bind && (point.bind >> r.geom) }).map(r => r)
      assertEquals(bean, q10.first)

      val q11 = GeomTests.filter(r => { r.id === bean.id.bind && (r.geom &<| line2.bind) }).map(r => r)
      assertEquals(bean, q11.first)

      val q12 = GeomTests.filter(r => { r.id === bean.id.bind && (line2.bind |&> r.geom) }).map(r => r)
      assertEquals(bean, q12.first)

      val q13 = GeomTests.filter(r => { r.id === bean.id.bind && (r.geom <<| point.bind) }).map(r => r)
      assertEquals(bean, q13.first)

      val q14 = GeomTests.filter(r => { r.id === bean.id.bind && (point.bind |>> r.geom) }).map(r => r)
      assertEquals(bean, q14.first)

      val q15 = {
        val latLongPoint = makePoint(point2.getX, point2.getY).setSRID(4326)
        val distanceQuery = PointTests.filter(_.id === pbean1.id.bind)
        val distance = distanceQuery.map(_.point.setSRID(4326).distanceSphere(latLongPoint)).first
        distanceQuery.map { p =>
          ( p.point.setSRID(4326).dWithin(latLongPoint, distance * 1.01, Some(true)),
            p.point.setSRID(4326).dWithin(latLongPoint, distance * 0.99, Some(true))) }.first
      }
      assertEquals((true, false), q15)
    }
  }

  @Test
  def testGeomAccessors(): Unit = {
    val point = wktReader.read("POINT(4 5 7)")
    val line = wktReader.read("LINESTRING(0 0, 3 3)")
    val polygon = wktReader.read("POLYGON((0 0, 1 1, 1 2, 1 1, 0 0))")
    val collection = wktReader.read("GEOMETRYCOLLECTION(LINESTRING(1 1,0 0),POINT(0 0))")

    db withSession { implicit session: Session =>
      val pointbean = GeometryBean(130L, point)
      val linebean = GeometryBean(131L, line)
      val polygonbean = GeometryBean(132L, polygon)
      val collectionbean = GeometryBean(133L, collection)
      GeomTests.forceInsertAll(pointbean, linebean, polygonbean, collectionbean)

      val q1 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.geomType)
      assertEquals("ST_LineString", q1.first)

      val q2 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.srid)
      assertEquals(0, q2.first)

      val q3 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.isValid)
      assertEquals(false, q3.first)

      val q4 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.isClosed)
      assertEquals(true, q4.first)

      val q5 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.isCollection)
      assertEquals(false, q5.first)

      val q6 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.isEmpty)
      assertEquals(false, q6.first)

      val q7 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.isRing)
      assertEquals(false, q7.first)

      val q8 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.isSimple)
      assertEquals(true, q8.first)

      val q9 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.hasArc)
      assertEquals(false, q9.first)

      val q10 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.area)
      assertEquals(0.0f, q10.first, 0.001f)

      val q11 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.boundary)
      assertEquals(wktReader.read("MULTIPOINT(0 0, 3 3)"), q11.first)

      val q12 = GeomTests.filter(_.id === collectionbean.id.bind).map(_.geom.dimension)
      assertEquals(1, q12.first)

      val q13 = GeomTests.filter(_.id === collectionbean.id.bind).map(_.geom.coordDim)
      assertEquals(2, q13.first)

      val q14 = GeomTests.filter(_.id === collectionbean.id.bind).map(_.geom.nDims)
      assertEquals(2, q14.first)

      val q15 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.nPoints)
      assertEquals(2, q15.first)

      val q16 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.nRings)
      assertEquals(1, q16.first)

      val q17 = GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.x)
      assertEquals(4f, q17.first, 0.001f)

      val q18 = GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.y)
      assertEquals(5f, q18.first, 0.001f)

      val q19 = GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.z.?)
      assertEquals(Some(7f), q19.first)

      val q20 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.xmin)
      assertEquals(0f, q20.first, 0.001f)

      val q21 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.xmax)
      assertEquals(1f, q21.first, 0.001f)

      val q22 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.ymin)
      assertEquals(0f, q22.first, 0.001f)

      val q23 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.ymax)
      assertEquals(2f, q23.first, 0.001f)

      val q24 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.zmin.?)
      assertEquals(Some(0.0), q24.first)

      val q25 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.zmax.?)
      assertEquals(Some(0.0), q25.first)
    }
  }

  @Test
  def testGeomOutputs(): Unit = {
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

    db withSession { implicit session: Session =>
      val bean = GeometryBean(141L, polygon)
      val bean1 = GeometryBean(142L, point)
      GeomTests.forceInsertAll(bean, bean1)

      val q1 = GeomTests.filter(_.id === bean.id.bind).map(_.geom.asText)
      assertEquals(POLYGON, q1.first)

      val q2 = GeomTests.filter(_.id === bean.id.bind).map(_.geom.asEWKT)
      assertEquals(POLYGON_EWKT, q2.first)

      val q3 = GeomTests.filter(_.id === bean.id.bind).map(_.geom.asHEXEWKB())
      assertEquals(POLYGON_HEX, q3.first)

      val q4 = GeomTests.filter(_.id === bean1.id.bind).map(_.geom.asLatLonText())
      assertEquals(POINT_LatLon, q4.first)

      val q5 = GeomTests.filter(_.id === bean.id.bind).map(_.geom.asGML())
      assertEquals(POLYGON_GML, q5.first)

      val q6 = GeomTests.filter(_.id === bean.id.bind).map(_.geom.setSRID(4326).asKML())
      assertEquals(POLYGON_KML, q6.first)

      val q7 = GeomTests.filter(_.id === bean.id.bind).map(_.geom.asGeoJSON())
      assertEquals(POLYGON_JSON, q7.first)

      val q8 = GeomTests.filter(_.id === bean.id.bind).map(_.geom.asSVG())
      assertEquals(POLYGON_SVG, q8.first)
    }
  }

  @Test
  def testGeomRelationships(): Unit = {
    val polygon = wktReader.read("POLYGON((175 150, 20 40, 50 60, 125 100, 175 150))")
    val multiPoints = wktReader.read("MULTIPOINT(125 100, 125 101)")
    val point = wktReader.read("POINT(175 150)")

    val line1 = wktReader.read("LINESTRING(0 0, 100 100)")
    val line2 = wktReader.read("LINESTRING(0 0, 5 5, 100 100)")

    db withSession { implicit session: Session =>
      val linebean = GeometryBean(151L, line1)
      val polygonbean = GeometryBean(152L, polygon)
      GeomTests.forceInsertAll(linebean, polygonbean)

      val q1 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.gEquals(line2.bind))
      assertEquals(true, q1.first)
      val q2 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.gEquals(line2.bind.reverse))
      assertEquals(true, q2.first)

      val q3 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.orderingEquals(line2.bind))
      assertEquals(false, q3.first)

      val q4 = GeomTests.filter(_.id === polygonbean.id.bind).map(
        _.geom.overlaps(geomFromText("LINESTRING(0 0, 10 10)".bind).buffer(10f.bind)))
      assertEquals(false, q4.first)

      val q5 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.intersects(line1.bind))
      assertEquals(true, q5.first)

      val q6 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.crosses(line1.bind))
      assertEquals(true, q6.first)

      val q7 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.disjoint(line1.bind))
      assertEquals(false, q7.first)

      val q8 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.contains(multiPoints.bind))
      assertEquals(true, q8.first)

      val q9 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.containsProperly(multiPoints.bind))
      assertEquals(false, q9.first)

      val q11 = GeomTests.filter(_.id === polygonbean.id.bind).map(r => multiPoints.bind.within(r.geom))
      assertEquals(true, q11.first)
      val q12 = GeomTests.filter(_.id === polygonbean.id.bind).map(r => multiPoints.bind.dWithin(r.geom, 10d.bind))
      assertEquals(true, q12.first)
      val q13 = GeomTests.filter(_.id === polygonbean.id.bind).map(r => point.bind.dFullyWithin(r.geom, 200d.bind))
      assertEquals(true, q13.first)

      val q14 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.touches(point.bind))
      assertEquals(true, q14.first)

      val q15 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.relate(multiPoints.bind, "T*****FF*".bind))
      assertEquals(true, q15.first)
      val q16 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.relatePattern(point.bind))
      assertEquals("FF20F1FF2", q16.first)
    }
  }

  @Test
  def testGeomMeasures(): Unit = {
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

    db withSession { implicit session: Session =>
      val pointbean = GeometryBean(161L, point1)
      val linebean = GeometryBean(162L, line)
      val line3dbean = GeometryBean(163L, line3d)
      val polygonbean = GeometryBean(164L, polygon)
      GeomTests.forceInsertAll(pointbean, linebean, line3dbean, polygonbean)
      //
      val pbean1 = PointBean(166L, point1)
      val pbean2 = PointBean(167L, point2)
      PointTests.forceInsertAll(pbean1, pbean2)

      val q1 = GeomTests.filter(_.id === pointbean.id.bind).map((_.geom.azimuth(point2.bind).toDegrees))
      assertEquals(42.2736890060937d, q1.first, 0.1d)

      val q2 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.centroid)
      assertEquals(centroid, q2.first)
      val q21 = GeomTests.filter(_.id === polygonbean.id.bind).map(r => (r.geom.centroid within r.geom))
      assertEquals(true, q21.first)

      val q3 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.closestPoint(point2.bind))
      assertEquals(closetPoint, q3.first)

      val q4 = GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.pointOnSurface)
      assertEquals(point1, q4.first)

      val q5 = GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.project(1000f.bind, 45f.bind.toRadians))
      assertEquals(projectedPoint, q5.first)

      val q6 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.length)
      assertEquals(122.630744000095d, q6.first, 0.1d)

      val q7 = GeomTests.filter(_.id === line3dbean.id.bind).map(_.geom.length3d)
      assertEquals(122.704716741457d, q7.first, 0.1d)

      val q8 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.perimeter)
      assertEquals(381.83197021484375d, q8.first, 0.1d)

      val q9 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.distance(point2.bind))
      assertEquals(17.09934425354004d, q9.first, 0.1d)
      val q91 = PointTests.sortBy(_.point.distance(point3.bind)).map(r => r)
      assertEquals(List(pbean1, pbean2), q91.list)

      val q10 = GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.distanceSphere(point3.bind))
      assertEquals(5286499.0d, q10.first, 0.1d)

      val q11 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.maxDistance(point2.bind))
      assertEquals(111.80339813232422d, q11.first, 0.1d)

      val q12 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.hausdorffDistance(line3d.bind))
      assertEquals(0.0d, q12.first, 0.1d)

      val q13 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.longestLine(point2.bind))
      assertEquals(longestLine, q13.first)

      val q14 = GeomTests.filter(_.id === polygonbean.id.bind).map(_.geom.shortestLine(point2.bind))
      assertEquals(shortestLine, q14.first)
    }
  }

  @Test
  def testGeomProcessing(): Unit = {
    val point = wktReader.read("POINT(-123.365556 48.428611)")
    val point1 = wktReader.read("POINT(3 1)")
    val line = wktReader.read("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1)")
    val line1 = wktReader.read("LINESTRING(1 2,2 1,3 1)")
    val bladeLine = wktReader.read("LINESTRING(0 4,4 0)")
    val multiLine = wktReader.read("MULTILINESTRING((-29 -27,-30 -29.7,-36 -31,-45 -33),(-45 -33,-46 -32))")
    val collection = wktReader.read("GEOMETRYCOLLECTION(GEOMETRYCOLLECTION(LINESTRING(0 0, 1 1)),LINESTRING(2 2, 3 3))")

    db withSession { implicit session: Session =>
      point.setSRID(4326)
      
      val pointbean = GeometryBean(171L, point)
      val linebean = GeometryBean(172L, line)
      val multilinebean = GeometryBean(173L, multiLine)
      val collectionbean = GeometryBean(174L, collection)
      GeomTests.forceInsertAll(pointbean, linebean, multilinebean, collectionbean)

      val q1 = GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.setSRID(0.bind).asEWKT)
      assertEquals("POINT(-123.365556 48.428611)", q1.first)

      val q2 = GeomTests.filter(_.id === pointbean.id.bind).map(_.geom.transform(26986.bind).asEWKT)
      assertEquals("SRID=26986;POINT(-3428094.64636768 2715245.01412978)", q2.first)

      val q3 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.simplify(0.5f.bind).asText)
      assertEquals("LINESTRING(1 1,2 2,2 3.5,1 3,2 1)", q3.first)

      val q4 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.simplifyPreserveTopology(0.5f.bind).asText)
      assertEquals("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1)", q4.first)

      val q5 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.removeRepeatedPoints.asText)
      assertEquals("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1)", q5.first)

      val q6 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.difference(line1.bind).asText)
      assertEquals("MULTILINESTRING((1 1,1.5 1.5),(1.5 1.5,2 2,2 3.5,1 3,1 2))", q6.first)

      val q7 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.symDifference(line1.bind).asText)
      assertEquals("MULTILINESTRING((1 1,1.5 1.5),(1.5 1.5,2 2,2 3.5,1 3,1 2),(2 1,3 1))", q7.first)

      val q8 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.intersection(line1.bind).asText)
      assertEquals("MULTILINESTRING((1 2,1.5 1.5),(1.5 1.5,2 1))", q8.first)

      val q9 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.sharedPaths(line1.bind).asText)
      assertEquals("GEOMETRYCOLLECTION(MULTILINESTRING((1 2,1.5 1.5),(1.5 1.5,2 1)),MULTILINESTRING EMPTY)", q9.first)

      val q10 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.split(bladeLine.bind).asText)
      assertEquals("GEOMETRYCOLLECTION(LINESTRING(1 1,1.5 1.5),LINESTRING(1.5 1.5,2 2),LINESTRING(2 2,2 3.5,1 3),LINESTRING(1 3,1 2,1.5 1.5),LINESTRING(1.5 1.5,2 1))", q10.first)

      val q11 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.minBoundingCircle(8.bind).asText)
      assertEquals("POLYGON((2.84629120178363 2.25,2.82042259384576 1.98735161591655,2.74381088612791 1.73479666193852,2.61940022359336 1.50204068331283,2.45197163823299 1.29802836176701,2.24795931668717 1.13059977640664,2.01520333806148 1.00618911387209,1.76264838408345 0.929577406154245,1.5 0.903708798216374,1.23735161591655 0.929577406154244,0.984796661938523 1.00618911387208,0.752040683312833 1.13059977640664,0.548028361767014 1.29802836176701,0.380599776406643 1.50204068331283,0.256189113872087 1.73479666193852,0.179577406154245 1.98735161591655,0.153708798216374 2.25,0.179577406154243 2.51264838408344,0.256189113872083 2.76520333806147,0.380599776406638 2.99795931668717,0.548028361767008 3.20197163823298,0.752040683312826 3.36940022359336,0.984796661938515 3.49381088612791,1.23735161591655 3.57042259384575,1.49999999999999 3.59629120178363,1.76264838408344 3.57042259384576,2.01520333806148 3.49381088612792,2.24795931668717 3.36940022359336,2.45197163823299 3.20197163823299,2.61940022359336 2.99795931668717,2.74381088612791 2.76520333806148,2.82042259384575 2.51264838408345,2.84629120178363 2.25))", q11.first)

      val q12 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.buffer(10f.bind).asText)
      assertEquals("POLYGON((-8.95075429832142 1.5,-9 2,-9 3,-8.83023553552631 4.83479408001984,-8.32670613678483 6.60729159315686,-7.5065080835204 8.25731112119134,-6.39748947238797 9.72882972781368,-5.03730469350959 10.9718850993806,-3.47213595499958 11.9442719099992,-2.47213595499958 12.4442719099992,-0.572769319290237 13.1633771544796,1.43271648611295 13.4838965046154,3.46160106847735 13.3926094796381,5.4301989043202 12.8932814009163,7.25731112119134 12.0065080835204,8.86757471241229 10.7688663194088,10.1945710395115 9.2314051923066,11.1835654057703 7.45754045310197,11.7937647015257 5.52043880658346,12 3.5,12 2,11.9507542983214 1.5,12 0.999999999999993,11.8078528040323 -0.950903220161287,11.2387953251129 -2.8268343236509,10.3146961230255 -4.55570233019602,9.07106781186548 -6.07106781186547,7.55570233019603 -7.31469612302545,5.82683432365091 -8.23879532511286,3.95090322016129 -8.8078528040323,2.00000000000001 -9,1.5 -8.95075429832142,0.999999999999994 -9,-0.950903220161286 -8.8078528040323,-2.8268343236509 -8.23879532511287,-4.55570233019602 -7.31469612302545,-6.07106781186547 -6.07106781186548,-7.31469612302545 -4.55570233019603,-8.23879532511286 -2.8268343236509,-8.8078528040323 -0.950903220161295,-9 0.99999999999999,-8.95075429832142 1.5))", q12.first)

      val q13 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.multi.asText)
      assertEquals("MULTILINESTRING((1 1,2 2,2 3.5,1 3,1 2,2 1))", q13.first)

      val q14 = GeomTests.filter(_.id === multilinebean.id.bind).map(_.geom.lineMerge.asText)
      assertEquals("LINESTRING(-29 -27,-30 -29.7,-36 -31,-45 -33,-46 -32)", q14.first)

      val q15 = GeomTests.filter(_.id === collectionbean.id.bind).map(_.geom.collectionExtract(2.bind).asText)
      assertEquals("MULTILINESTRING((0 0,1 1),(2 2,3 3))", q15.first)

      val q16 = GeomTests.filter(_.id === collectionbean.id.bind).map(_.geom.collectionHomogenize)
      assertEquals("MULTILINESTRING ((0 0, 0 1), (2 2, 3 3))", wktWriter.write(q16.first))

      val q17 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.addPoint(point1.bind).asText)
      assertEquals("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1,3 1)", q17.first)

      val q18 = GeomTests.filter(_.id === linebean.id.bind).map(r => r.geom.setPoint(point1.bind, (r.geom.nPoints - 1.bind)).asText)
      assertEquals("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,3 1)", q18.first)

      val q19 = GeomTests.filter(_.id === linebean.id.bind).map(r => r.geom.removePoint((r.geom.nPoints - 1.bind)).asText)
      assertEquals("LINESTRING(1 1,2 2,2 3.5,1 3,1 2)", q19.first)

      val q20 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.scale(1f.bind, 2f.bind).asText)
      assertEquals("LINESTRING(1 2,2 4,2 7,1 6,1 4,2 2)", q20.first)

      val q21 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.segmentize((5f.bind)).asText)
      assertEquals("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1)", q21.first)

      val q22 = GeomTests.filter(_.id === linebean.id.bind).map(r => r.geom.snap(line1.bind, r.geom.distance(line1.bind)).asText)
      assertEquals("LINESTRING(1 1,2 2,2 3.5,1 3,1 2,2 1)", q22.first)

      val q23 = GeomTests.filter(_.id === linebean.id.bind).map(_.geom.scale(1f.bind, 2f.bind).asText)
      assertEquals("LINESTRING(1 2,2 4,2 7,1 6,1 4,2 2)", q23.first)
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      Try { (GeomTests.ddl ++ PointTests.ddl) drop }
      Try { (GeomTests.ddl ++ PointTests.ddl).createStatements.foreach(s => println(s"[jts] $s")) }
      Try { (GeomTests.ddl ++ PointTests.ddl) create }
    }
  }
}
