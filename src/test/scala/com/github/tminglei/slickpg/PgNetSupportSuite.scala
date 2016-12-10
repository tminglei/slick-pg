package com.github.tminglei.slickpg

import org.scalatest.FunSuite

import slick.jdbc.GetResult

import scala.concurrent.Await
import scala.concurrent.duration._

class PgNetSupportSuite extends FunSuite {
  import MyPostgresProfile.api._

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

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

  test("InetString spec") {
    // ipv4
    val ipv4net = InetString("192.168.1.5/24")
    assert(false === ipv4net.isIPv6)
    assert("192.168.1.5" === ipv4net.address)
    assert(24 === ipv4net.masklen)

    // ipv6
    val ipv6net = InetString("2001:4f8:3:ba:2e0:81ff:fe22:d1f1")
    assert(true === ipv6net.isIPv6)
    assert("2001:4f8:3:ba:2e0:81ff:fe22:d1f1" === ipv6net.address)
    assert(128 === ipv6net.masklen)
  }

  test("Net Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        NetTests.schema create,
        ///
        NetTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          NetTests.to[List].result.map(
            r => assert(List(testRec1, testRec2, testRec3) === r)
          ),
          // <<
          NetTests.filter(_.id === 35L.bind).map(_.inet << InetString("192.168.1.5/20")).result.head.map(
            r => assert(true === r)
          ),
          // <<=
          NetTests.filter(_.inet <<= InetString("192.168.1.5/24")).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          // >>
          NetTests.filter(_.inet >> InetString("10.1.0.1/24")).to[List].result.map(
            r => assert(List(testRec1) === r)
          ),
          // >>=
          NetTests.filter(_.inet >>= InetString("10.1.0.1/16")).to[List].result.map(
            r => assert(List(testRec1) === r)
          ),
          // &&
          NetTests.filter(_.inet &&& InetString("192.168.1.15/24")).to[List].result.map(
            r => assert(List(testRec2) === r)
          ),
          // ~
          NetTests.filter(_.id === 33L).map(~ _.inet).result.head.map(
            r => assert(InetString("245.254.255.255/16") === r)
          ),
          // &
          NetTests.filter(_.id === 33L).map(~ _.inet).result.head.map(
            r => assert(InetString("245.254.255.255/16") === r)
          ),
          // |
          NetTests.filter(_.id === 35L).map(_.inet | InetString("192.168.23.20/30")).result.head.map(
            r => assert(InetString("192.168.23.21/30") === r)
          ),
          // +
          NetTests.filter(_.id === 35L).map(_.inet + 35).result.head.map(
            r => assert(InetString("192.168.1.40/24") === r)
          ),
          // -
          NetTests.filter(_.id === 37L).map(_.inet - 101).result.head.map(
            r => assert(InetString("2001:4f8:3:ba:2e0:81ff:fe22:d18c/127") === r)
          ),
          NetTests.filter(_.id === 37L).map(_.inet -- InetString("2001:4f8:3:ba:2e0:81ff:fe22:d18c/32")).result.head.map(
            r => assert(101 === r)
          ),
          // abbrev
          NetTests.filter(_.id === 33L).map(_.inet abbrev).result.head.map(
            r => assert("10.1.0.0/16" === r)
          ),
          // broadcast
          NetTests.filter(_.id === 35L).map(_.inet broadcast).result.head.map(
            r => assert(InetString("192.168.1.255/24") === r)
          ),
          // family
          NetTests.filter(_.id === 37L).map(_.inet family).result.head.map(
            r => assert(6 === r)
          ),
          // host
          NetTests.filter(_.id === 35L).map(_.inet host).result.head.map(
            r => assert("192.168.1.5" === r)
          ),
          // hostmask
          NetTests.filter(_.id === 35L).map(_.inet hostmask).result.head.map(
            r => assert(InetString("0.0.0.255") === r)
          ),
          // masklen
          NetTests.filter(_.id === 37L).map(_.inet masklen).result.head.map(
            r => assert(127 === r)
          ),
          // netmask
          NetTests.filter(_.id === 35L).map(_.inet netmask).result.head.map(
            r => assert(InetString("255.255.255.0") === r)
          ),
          // network
          NetTests.filter(_.id === 35L).map(_.inet network).result.head.map(
            r => assert(InetString("192.168.1.0/24") === r)
          ),
          // text
          NetTests.filter(_.id === 37L).map(_.inet text).result.head.map(
            r => assert("2001:4f8:3:ba:2e0:81ff:fe22:d1f1/127" === r)
          ),
          // set_masklen
          NetTests.filter(_.id === 35L).map(_.inet.setMasklen(16)).result.head.map(
            r => assert(InetString("192.168.1.5/16") === r)
          )
        )
      ).andFinally(
        NetTests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  test("MacAddr Lifted support") {
    Await.result(db.run(
      DBIO.seq(
        NetTests.schema create,
        ///
        NetTests forceInsertAll List(testRec1, testRec2, testRec3)
      ).andThen(
        DBIO.seq(
          // ~
          NetTests.filter(_.id === 33L).map(~ _.mac).result.head.map(
            r => assert(Some(MacAddrString("ed:cb:a9:87:6f:54")) === r)
          ),
          // &
          NetTests.filter(_.id === 33L.bind).map(_.mac & MacAddrString("08:00:2b:01:02:03")).result.head.map(
            r => assert(Some(MacAddrString("00:00:02:00:00:03")) === r)
          ),
          // |
          NetTests.filter(_.id === 33L).map(_.mac | MacAddrString("08:00:2b:01:02:03")).result.head.map(
            r => assert(Some(MacAddrString("1a:34:7f:79:92:ab")) === r)
          ),
          // trunc
          NetTests.filter(_.id === 33L).map(_.mac trunc).result.head.map(
            r => assert(Some(MacAddrString("12:34:56:00:00:00")) === r)
          )
        )
      ).andFinally(
        NetTests.schema drop
      ).transactionally
    ), Duration.Inf)
  }

  //------------------------------------------------------------------------------

  test("net Plain SQL support") {
    import MyPostgresProfile.plainAPI._

    implicit val getNetBeanResult = GetResult(r => NetBean(r.nextLong(), r.nextIPAddr(), r.nextMacAddrOption()))

    val b = NetBean(34L, InetString("10.1.0.0/16"), Some(MacAddrString("12:34:56:78:90:ab")))

    Await.result(db.run(DBIO.seq(
      sqlu"""create table net_test(
              id int8 not null primary key,
              inet inet not null,
              mac macaddr)
          """,
      ///
      sqlu""" insert into net_test values(${b.id}, ${b.inet}, ${b.mac}) """,
      sql""" select * from net_test where id = ${b.id} """.as[NetBean].head.map(
        r => assert(b === r)
      ),
      ///
      sqlu"drop table if exists net_test cascade"
    ).transactionally), Duration.Inf)
  }
}
