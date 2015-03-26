package com.github.tminglei.slickpg

import org.junit._
import org.junit.Assert._

import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class PgNetSupportTest {
  import MyPostgresDriver.api._

  val db = Database.forURL(url = dbUrl, driver = "org.postgresql.Driver")

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
    Await.result(db.run(DBIO.seq(
      NetTests.schema create,
      ///
      NetTests forceInsertAll List(testRec1, testRec2, testRec3),
      NetTests.to[List].result.map(
        assertEquals(List(testRec1, testRec2, testRec3), _)
      ),
      //-- test inet extension methods
      // 1.1. '<<'/'<<='/'>>'/'>>='
      NetTests.filter(_.id === 35L.bind).map(_.inet << InetString("192.168.1.5/20")).result.head.map(
        assertEquals(true, _)
      ),
      NetTests.filter(_.inet <<= InetString("192.168.1.5/24")).to[List].result.map(
        assertEquals(List(testRec2), _)
      ),
      NetTests.filter(_.inet >> InetString("10.1.0.1/24")).to[List].result.map(
        assertEquals(List(testRec1), _)
      ),
      NetTests.filter(_.inet >>= InetString("10.1.0.1/16")).to[List].result.map(
        assertEquals(List(testRec1), _)
      ),
      // 1.2. '&&'/'~'/'&'/'|'/'+'/'-'
      NetTests.filter(_.inet &&& InetString("192.168.22.5/24")).to[List].result.map(
        assertEquals(List(testRec2), _)
      ),
      NetTests.filter(_.id === 33L).map(~ _.inet).result.head.map(
        assertEquals(InetString("245.254.255.255/16"), _)
      ),
      NetTests.filter(_.id === 33L.bind).map(_.inet & InetString("0.0.0.255")).result.head.map(
        assertEquals(InetString("0.0.0.0"), _)
      ),
      NetTests.filter(_.id === 35L).map(_.inet | InetString("192.168.23.20/30")).result.head.map(
        assertEquals(InetString("192.168.23.21/30"), _)
      ),
      NetTests.filter(_.id === 35L).map(_.inet + 35).result.head.map(
        assertEquals(InetString("192.168.1.40/24"), _)
      ),
      NetTests.filter(_.id === 37L).map(_.inet - 101).result.head.map(
        assertEquals(InetString("2001:4f8:3:ba:2e0:81ff:fe22:d18c/127"), _)
      ),
      NetTests.filter(_.id === 37L).map(_.inet -- InetString("2001:4f8:3:ba:2e0:81ff:fe22:d18c/32")).result.head.map(
        assertEquals(101, _)
      ),
      // 1.3. 'abbrev'/'broadcast'/'family'/'host'
      NetTests.filter(_.id === 33L).map(_.inet abbrev).result.head.map(
        assertEquals("10.1.0.0/16", _)
      ),
      NetTests.filter(_.id === 35L).map(_.inet broadcast).result.head.map(
        assertEquals(InetString("192.168.1.255/24"), _)
      ),
      NetTests.filter(_.id === 37L).map(_.inet family).result.head.map(
        assertEquals(6, _)
      ),
      NetTests.filter(_.id === 35L).map(_.inet host).result.head.map(
        assertEquals("192.168.1.5", _)
      ),
      // 1.4. 'hostmask'/'masklen'/'netmask'/'network'/'text'/'set_masklen'
      NetTests.filter(_.id === 35L).map(_.inet hostmask).result.head.map(
        assertEquals(InetString("0.0.0.255"), _)
      ),
      NetTests.filter(_.id === 37L).map(_.inet masklen).result.head.map(
        assertEquals(127, _)
      ),
      NetTests.filter(_.id === 35L).map(_.inet netmask).result.head.map(
        assertEquals(InetString("255.255.255.0"), _)
      ),
      NetTests.filter(_.id === 35L).map(_.inet network).result.head.map(
        assertEquals(InetString("192.168.1.0/24"), _)
      ),
      NetTests.filter(_.id === 37L).map(_.inet text).result.head.map(
        assertEquals("2001:4f8:3:ba:2e0:81ff:fe22:d1f1/127", _)
      ),
      NetTests.filter(_.id === 35L).map(_.inet.setMasklen(16)).result.head.map(
        assertEquals(InetString("192.168.1.5/16"), _)
      ),
      //-- test mac addr extension methods
      NetTests.filter(_.id === 33L).map(~ _.mac).result.head.map(
        assertEquals(Some(MacAddrString("ed:cb:a9:87:6f:54")), _)
      ),
      NetTests.filter(_.id === 33L.bind).map(_.mac & MacAddrString("08:00:2b:01:02:03")).result.head.map(
        assertEquals(Some(MacAddrString("00:00:02:00:00:03")), _)
      ),
      NetTests.filter(_.id === 33L).map(_.mac | MacAddrString("08:00:2b:01:02:03")).result.head.map(
        assertEquals(Some(MacAddrString("1a:34:7f:79:92:ab")), _)
      ),
      NetTests.filter(_.id === 33L).map(_.mac trunc).result.head.map(
        assertEquals(Some(MacAddrString("12:34:56:00:00:00")), _)
      ),
      ///
      NetTests.schema drop
    ).transactionally), Duration.Inf)
  }

  //------------------------------------------------------------------------------

  @Test
  def testPlainNetFunctions(): Unit = {
    import MyPlainPostgresDriver.plainAPI._

    implicit val getNetBeanResult = GetResult(r => NetBean(r.nextLong(), r.nextIPAddr(), r.nextMacAddrOption()))

    val b = NetBean(33L, InetString("10.1.0.0/16"), Some(MacAddrString("12:34:56:78:90:ab")))

    Await.result(db.run(DBIO.seq(
      sqlu"""create table net_test(
              id int8 not null primary key,
              inet inet not null,
              mac macaddr)
          """,
      ///
      sqlu"insert into net_test values(${b.id}, ${b.inet}, ${b.mac})",
      sql"select * from net_test where id = ${b.id}".as[NetBean].head.map(
        assertEquals(b, _)
      ),
      ///
      sqlu"drop table if exists net_test cascade"
    ).transactionally), Duration.Inf)
  }
}
