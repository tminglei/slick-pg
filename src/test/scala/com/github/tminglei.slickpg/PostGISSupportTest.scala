package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._
import com.vividsolutions.jts.geom.{Point, Geometry}
import com.vividsolutions.jts.io.{WKBWriter, WKTReader}

class PostGISSupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class GeometryBean(id: Long, geom: Geometry)

  object GeomTestTable extends Table[GeometryBean](Some("test"), "geom_test") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def geom = column[Geometry]("geom")

    def * = id ~ geom <> (GeometryBean, GeometryBean.unapply _)
    def iCols = geom returning id
  }

  ////////////////////////////////////////////////////////////////////////////////

  val wktReader = new WKTReader()
  val wkbWriter = new WKBWriter(2, true)

  @Test
  def testGeomConstructors(): Unit = {
    val POINT = "POINT(-71.064544 42.28787)"
    val point = wktReader.read(POINT)

    db withSession { implicit session: Session =>
      val id = GeomTestTable.iCols.insert(point)
      val bean = GeometryBean(id, point)

      val q0 = GeomTestTable.where(_.id === id.bind).map(t => t)
      assertEquals(bean, q0.first())

      val q1 = GeomTestTable.where(_.geom === geomFromText(POINT.bind)).map(t => t)
      assertEquals(bean, q1.first())

      val q2 = GeomTestTable.where(_.geom === geomFromWKB(wkbWriter.write(point).bind)).map(t => t)
      assertEquals(bean, q2.first())

      val q3 = GeomTestTable.where(_.geom === geomFromEWKT("SRID=0;POINT(-71.064544 42.28787)".bind)).map(t => t)
      assertEquals(bean, q3.first())

      val q4 = GeomTestTable.where(_.geom === geomFromGML("""<gml:Point>
                                                            <gml:coordinates>-71.064544,42.28787</gml:coordinates>
                                                       </gml:Point>""".bind)).map(t => t)
      assertEquals(bean, q4.first())

      val q5 = GeomTestTable.where(_.geom === geomFromKML("""<Point>
                                                            <coordinates>-71.064544,42.28787</coordinates>
                                                       </Point>""".bind).setSRID(0.bind)).map(t => t)
      assertEquals(bean, q5.first())

      // disable it, since JSON-C not enabled
//      val q6 = GeomTestTable.where(_.geom === geomFromGeoJSON("""{"type":"Point","coordinates":[-71.064544,42.28787]}""".bind)).map(t => t)
//      assertEquals(bean, q6.first())

      GeomTestTable.where(_.id === id.bind).delete
    }
  }

  @Test
  def testGeomOperators(): Unit = {
    val line1 = wktReader.read("LINESTRING(0 0, 3 3)")
    val line2 = wktReader.read("LINESTRING(1 2, 4 6)")
    val line3 = wktReader.read("LINESTRING (1 1, 2 2)")
    val line3d1 = wktReader.read("LINESTRING(0 0 1, 3 3 2)")
    val line3d2 = wktReader.read("LINESTRING(1 2 1, 4 6 1)")

    db withSession { implicit session: Session =>
      val id = GeomTestTable.iCols.insert(line1)
      val bean = GeometryBean(id, line1)
      val id3d = GeomTestTable.iCols.insert(line3d1)
      val bean3d = GeometryBean(id3d, line3d1)

      ///
      val q1 = GeomTestTable.where(r => { r.id === id.bind && r.geom @&& line2.bind }).map(r => r)
      assertEquals(bean, q1.first())

      val q2 = GeomTestTable.where(r => { r.id === id3d.bind && r.geom @&&& line3d2.bind }).map(r => r)
      assertEquals(bean3d, q2.first())

      val q3 = GeomTestTable.where(r => { r.id === id.bind && r.geom @> line3.bind }).map(r => r)
      assertEquals(bean, q3.first())

      val q4 = GeomTestTable.where(r => { r.id === id.bind && line3.bind <@ r.geom }).map(r => r)
      assertEquals(bean, q4.first())

      val q5 = GeomTestTable.where(r => { r.id === id.bind && (r.geom <-> line2.bind) > 0.7d.bind }).map(r => r)
      assertEquals(bean, q5.first())

      val q6 = GeomTestTable.where(r => { r.id === id.bind && (r.geom <#> line2.bind) === 0.0d.bind}).map(r => r)
      assertEquals(bean, q6.first())

      ///
    }
  }

  //////////////////////////////////////////////////////////////////////

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      GeomTestTable.ddl create
    }
  }

  @After
  def dropTables(): Unit = {
    db withSession { implicit session: Session =>
      GeomTestTable.ddl drop
    }
  }
}
