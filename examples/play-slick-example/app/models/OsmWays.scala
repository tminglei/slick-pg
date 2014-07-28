package models

import com.vividsolutions.jts.geom.Point
import org.joda.time.LocalDateTime
import play.api.libs.json.JsValue
import myUtils.WithMyDriver

case class OsmWay(
  id: Int,
  version:Int,
  userId: Int,
  location: Point,
  tstamp: LocalDateTime,
  changeSetId: Int,
  tags: Map[String, String],
  nodes: List[Int],
  others: Option[JsValue]
  )

/**
  * This OsmWays component contains the database representation of your
  * furry friends
  *
  * This pattern is called the cake pattern (I think it is because
  * it tastes good :P),
  *
  * Do not worry about the scary and yummy name, it is easily copyable!
  *
  * Just follow the steps
  * for each Table you want to have:
  *  1. the play.api.db.slick.Profile "self-type" (see below for an example)
  *  2. the import profile.simple._
  *
  * The reason you want to use the cake pattern here is because
  * we imagine we have multiple different databases for production
  * and tests
  */
trait OsmWaysComponent extends WithMyDriver {
  import driver.simple._

  class OsmWays(tag: Tag) extends Table[OsmWay](tag, "WAYS") {
    def id = column[Int]("id")
    def version = column[Int]("version")
    def userId = column[Int]("user_id")
    def location = column[Point]("location")
    def tstamp = column[LocalDateTime]("tstamp")
    def changeSetId = column[Int]("changeset_id")
    def tags = column[Map[String,String]]("tags")
    def nodes = column[List[Int]]("nodes")
    def others = column[Option[JsValue]]("others")

    def * = (id, version, userId, location, tstamp, changeSetId, tags, nodes, others) <> (OsmWay.tupled, OsmWay.unapply)
  }
}
