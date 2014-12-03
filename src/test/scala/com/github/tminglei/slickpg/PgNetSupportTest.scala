package com.github.tminglei.slickpg

import org.junit.Assert._
import org.junit.{Test, Before}

import scala.util.Try

class PgNetSupportTest {
  import com.github.tminglei.slickpg.MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=postgres", driver = "org.postgresql.Driver")

  case class NetBean(id: Long, inet: InetString, mac: Option[MacAddrString] = None)

  class NetTestTable(tag: Tag) extends Table[NetBean](tag, "net_test") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def inet = column[InetString]("inet")
    def mac = column[Option[MacAddrString]]("mac")

    def * = (id, inet, mac) <> (NetBean.tupled, NetBean.unapply)
  }
  val NetTests = TableQuery[NetTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = NetBean(33L, InetString("10.1.0.0/16"), Some(MacAddrString("12:34:56:78:90:ab")))
  val testRec2 = NetBean(35L, InetString("192.168.1.5/24"))
  val testRec3 = NetBean(37L, InetString("2001:4f8:3:ba:2e0:81ff:fe22:d1f1/127"))

  @Test
  def testInetStringMethods(): Unit = {
    val ipv4net = InetString("192.168.1.5/24")
    assertEquals(false, ipv4net.isIPv6)
    assertEquals("192.168.1.5", ipv4net.address)
    assertEquals(24, ipv4net.masklen)

    val ipv6net = InetString("2001:4f8:3:ba:2e0:81ff:fe22:d1f1")
    assertEquals(true, ipv6net.isIPv6)
    assertEquals("2001:4f8:3:ba:2e0:81ff:fe22:d1f1", ipv6net.address)
    assertEquals(128, ipv6net.masklen)
  }

  @Test
  def testNetFunctions(): Unit = {
    db withSession { implicit session: Session =>
      NetTests forceInsertAll (testRec1, testRec2, testRec3)

      val q0 = NetTests.map(r => r)
      assertEquals(List(testRec1, testRec2, testRec3), q0.list)

      //-- test inet extension methods
      val q1 = NetTests.filter(_.id === 35L.bind).map(_.inet << InetString("192.168.1.5/20"))
      println(s"[inet] '<<' sql = ${q1.selectStatement}")
      assertEquals(true, q1.first)

      val q2 = NetTests.filter(_.inet <<= InetString("192.168.1.5/24")).map(r => r)
      println(s"[inet] '<<=' sql = ${q2.selectStatement}")
      assertEquals(List(testRec2), q2.list)

      val q3 = NetTests.filter(_.inet >> InetString("10.1.0.1/24")).map(r => r)
      println(s"[inet] '>>' sql = ${q3.selectStatement}")
      assertEquals(List(testRec1), q3.list)

      val q4 = NetTests.filter(_.inet >>= InetString("10.1.0.1/16")).map(r => r)
      println(s"[inet] '>>=' sql = ${q4.selectStatement}")
      assertEquals(List(testRec1), q4.list)

      // start support from postgres 9.4
//      val q5 = NetTests.filter(_.inet &&& InetString("192.168.22.5/24")).map(r => r)
//      println(s"[inet] &&' sql = ${q5.selectStatement}")
//      assertEquals(List(testRec2), q5.list)

      val q6 = NetTests.filter(_.id === 33L).map(~ _.inet)
      println(s"[inet] '~' sql = ${q6.selectStatement}")
      assertEquals(InetString("245.254.255.255/16"), q6.first)

      val q7 = NetTests.filter(_.id === 33L.bind).map(_.inet & InetString("0.0.0.255"))
      println(s"[inet] '&' sql = ${q7.selectStatement}")
      assertEquals(InetString("0.0.0.0"), q7.first)

      val q8 = NetTests.filter(_.id === 35L).map(_.inet | InetString("192.168.23.20/30"))
      println(s"[inet] '|' sql = ${q8.selectStatement}")
      assertEquals(InetString("192.168.23.21/30"), q8.first)

      val q9 = NetTests.filter(_.id === 35L).map(_.inet + 35)
      println(s"[inet] '+' sql = ${q9.selectStatement}")
      assertEquals(InetString("192.168.1.40/24"), q9.first)

      val q10 = NetTests.filter(_.id === 37L).map(_.inet - 101)
      println(s"[inet] '-' sql = ${q10.selectStatement}")
      assertEquals(InetString("2001:4f8:3:ba:2e0:81ff:fe22:d18c/127"), q10.first)

      val q11 = NetTests.filter(_.id === 37L).map(_.inet -- InetString("2001:4f8:3:ba:2e0:81ff:fe22:d18c/32"))
      println(s"[inet] '--' sql = ${q11.selectStatement}")
      assertEquals(101, q11.first)

      val q12 = NetTests.filter(_.id === 33L).map(_.inet abbrev)
      println(s"[inet] 'abbrev' sql = ${q12.selectStatement}")
      assertEquals("10.1.0.0/16", q12.first)

      val q13 = NetTests.filter(_.id === 35L).map(_.inet broadcast)
      println(s"[inet] 'broadcast' sql = ${q13.selectStatement}")
      assertEquals(InetString("192.168.1.255/24"), q13.first)

      val q14 = NetTests.filter(_.id === 37L).map(_.inet family)
      println(s"[inet] 'family' sql = ${q14.selectStatement}")
      assertEquals(6, q14.first)

      val q15 = NetTests.filter(_.id === 35L).map(_.inet host)
      println(s"[inet] 'host' sql = ${q15.selectStatement}")
      assertEquals("192.168.1.5", q15.first)

      val q16 = NetTests.filter(_.id === 35L).map(_.inet hostmask)
      println(s"[inet] 'hostmask' sql = ${q16.selectStatement}")
      assertEquals(InetString("0.0.0.255"), q16.first)

      val q17 = NetTests.filter(_.id === 37L).map(_.inet masklen)
      println(s"[inet] 'masklen' sql = ${q17.selectStatement}")
      assertEquals(127, q17.first)

      val q18 = NetTests.filter(_.id === 35L).map(_.inet netmask)
      println(s"[inet] 'netmask' sql = ${q18.selectStatement}")
      assertEquals(InetString("255.255.255.0"), q18.first)

      val q19 = NetTests.filter(_.id === 35L).map(_.inet network)
      println(s"[inet] 'network' sql = ${q19.selectStatement}")
      assertEquals(InetString("192.168.1.0/24"), q19.first)

      val q21 = NetTests.filter(_.id === 37L).map(_.inet text)
      println(s"[inet] 'text' sql = ${q21.selectStatement}")
      assertEquals("2001:4f8:3:ba:2e0:81ff:fe22:d1f1/127", q21.first)

      val q20 = NetTests.filter(_.id === 35L).map(_.inet.setMasklen(16))
      println(s"[inet] 'set_masklen' sql = ${q20.selectStatement}")
      assertEquals(InetString("192.168.1.5/16"), q20.first)

      //-- test mac addr extension methods
      val q22 = NetTests.filter(_.id === 33L).map(~ _.mac)
      println(s"[mac] '~' sql = ${q22.selectStatement}")
      assertEquals(Some(MacAddrString("ed:cb:a9:87:6f:54")), q22.first)

      val q23 = NetTests.filter(_.id === 33L.bind).map(_.mac & MacAddrString("08:00:2b:01:02:03"))
      println(s"[mac] '&' sql = ${q23.selectStatement}")
      assertEquals(Some(MacAddrString("00:00:02:00:00:03")), q23.first)

      val q24 = NetTests.filter(_.id === 33L).map(_.mac | MacAddrString("08:00:2b:01:02:03"))
      println(s"[mac] '|' sql = ${q24.selectStatement}")
      assertEquals(Some(MacAddrString("1a:34:7f:79:92:ab")), q24.first)

      val q25 = NetTests.filter(_.id === 33L).map(_.mac trunc)
      println(s"[mac] 'trunc' sql = ${q25.selectStatement}")
      assertEquals(Some(MacAddrString("12:34:56:00:00:00")), q25.first)
    }
  }

  //------------------------------------------------------------------------------

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      Try { NetTests.ddl drop }
      Try { NetTests.ddl create }
    }
  }
}
