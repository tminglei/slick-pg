package com.github.tminglei.slickpg

import java.net.InetAddress
import org.junit.{Before, Test}
import scala.util.Try

class PgInetSupportTest {
  import com.github.tminglei.slickpg.MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=postgres", driver = "org.postgresql.Driver")

  case class InetBean(id: Long, addr: InetAddress)

  class InetTestTable(tag: Tag) extends Table[InetBean](tag, "InetTest0") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def addr = column[InetAddress]("inet", O.Default(InetAddress.getLocalHost))

    def * = (id, addr) <> (InetBean.tupled, InetBean.unapply)
  }
  val InetTests = TableQuery[InetTestTable]

  //------------------------------------------------------------------------------

  val testRec1 = InetBean(33L, InetAddress.getByName("127.0.0.1"))
  val testRec2 = InetBean(35L, InetAddress.getLocalHost)

  @Test
  def testInetFunctions(): Unit = {
    db withSession { implicit session: Session =>
      InetTests forceInsertAll(testRec1, testRec2)
    }
  }

  @Before
  def createTables(): Unit = {
    db withSession { implicit session: Session =>
      Try { InetTests.ddl drop }
      Try { InetTests.ddl create }
    }
  }
}
