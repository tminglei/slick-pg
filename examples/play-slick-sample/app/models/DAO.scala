package models

import play.api.db.slick.Config
import my.utils.MyPostgresDriver

class DAO(val driver: MyPostgresDriver) extends OsmWaysComponent {
  import driver.simple._
  
  val osmWays = TableQuery[OsmWays]
}

object current {
  lazy val dao = new DAO(Config.driver.asInstanceOf[MyPostgresDriver])
}
