package dao

import javax.inject.{Inject, Singleton}

import play.api.libs.json.JsValue
import util.MyPostgresDriver

import scala.concurrent.{ExecutionContext, Future}

import models.Company
import play.api.db.slick.{HasDatabaseConfigProvider, DatabaseConfigProvider, HasDatabaseConfig}

trait CompaniesComponent { self: HasDatabaseConfig[MyPostgresDriver] =>
  import driver.api._

  class Companies(tag: Tag) extends Table[Company](tag, "COMPANY") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def props = column[JsValue]("PROPS")

    def * = (id.?, name, props) <> (Company.tupled, Company.unapply _)
  }
}

@Singleton
class CompaniesDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends CompaniesComponent with HasDatabaseConfigProvider[MyPostgresDriver] {

  import driver.api._

  val companies = TableQuery[Companies]

  /** Construct the Map[String,String] needed to fill a select options set */
  def options(): Future[Seq[(String, String)]] = {
    val query = (for {
      company <- companies
    } yield (company.id, company.name)).sortBy(/*name*/_._2)

    db.run(query.result).map(rows => rows.map { case (id, name) => (id.toString, name) })
  }

  /** Insert a new company */
  def insert(company: Company): Future[Unit] =
    db.run(companies += company).map(_ => ())

  /** Insert new companies */
  def insert(companies: Seq[Company]): Future[Unit] =
    db.run(this.companies ++= companies).map(_ => ())
}