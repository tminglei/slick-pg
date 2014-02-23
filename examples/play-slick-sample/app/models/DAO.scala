package models

import play.api.db.slick.DB
import myUtils.MyPostgresDriver

class DAO(override val driver: MyPostgresDriver) extends OsmWaysComponent {
  import driver.simple._

  val osmWays = TableQuery(new OsmWays(_))
}

object current {
  val dao = new DAO(DB(play.api.Play.current).driver.asInstanceOf[MyPostgresDriver])
}
