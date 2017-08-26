package dao

import java.time.LocalDate
import java.util.Date
import javax.inject.{Inject,Singleton}

import util.MyPostgresDriver

import scala.concurrent.{ExecutionContext, Future}
import models.{ Company, Computer, Page }
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider}

@Singleton
class ComputersDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends CompaniesComponent
    with HasDatabaseConfigProvider[MyPostgresDriver] {
  import driver.api._

  class Computers(tag: Tag) extends Table[Computer](tag, "COMPUTER") {

    implicit val dateColumnType = MappedColumnType.base[Date, Long](d => d.getTime, d => new Date(d))

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def introduced = column[Option[LocalDate]]("INTRODUCED")
    def discontinued = column[Option[LocalDate]]("DISCONTINUED")
    def companyId = column[Option[Long]]("COMPANY_ID")

    def * = (id.?, name, introduced, discontinued, companyId) <> (Computer.tupled, Computer.unapply _)
  }

  private val computers = TableQuery[Computers]
  private val companies = TableQuery[Companies]

  /** Retrieve a computer from the id. */
  def findById(id: Long): Future[Option[Computer]] =
    db.run(computers.filter(_.id === id).result.headOption)

  /** Count all computers. */
  def count(): Future[Int] =
    db.run(computers.length.result)

  /** Count computers with a filter. */
  def count(filter: String): Future[Int] =
    db.run(computers.filter { computer => computer.name.toLowerCase like filter.toLowerCase }.length.result)

  /** Return a page of (Computer,Company) */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Future[Page[(Computer, Company)]] = {

    val offset = pageSize * page
    val query =
      (for {
        (computer, company) <- computers joinLeft companies on (_.companyId === _.id)
        if computer.name.toLowerCase like filter.toLowerCase
      } yield (computer, company.map(_.id), company.map(_.name), company.map(_.props)))
        .drop(offset)
        .take(pageSize)

    for {
      totalRows <- count(filter)
      list = query.result.map { rows => rows.collect { case (computer, id, Some(name), Some(props)) => (computer, Company(id, name, props)) } }
      result <- db.run(list)
    } yield Page(result, page, offset, totalRows)
  }

  /** Insert a new computer. */
  def insert(computer: Computer): Future[Unit] =
    db.run(computers += computer).map(_ => ())

  /** Insert new computers. */
  def insert(computers: Seq[Computer]): Future[Unit] =
    db.run(this.computers ++= computers).map(_ => ())

  /** Update a computer. */
  def update(id: Long, computer: Computer): Future[Unit] = {
    val computerToUpdate: Computer = computer.copy(Some(id))
    db.run(computers.filter(_.id === id).update(computerToUpdate)).map(_ => ())
  }

  /** Delete a computer. */
  def delete(id: Long): Future[Unit] =
    db.run(computers.filter(_.id === id).delete).map(_ => ())

}