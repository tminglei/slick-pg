package com.example

import java.sql.Timestamp

object PostGis extends App {
  import MyPostgresDriver.simple._

  implicit val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")

  case class OsmWay(id: Int, version: Int, user_id: Int, tstamp: Timestamp, changeset_id: Int, tags: Map[String, String], nodes: List[Int])

  class OsmWays(tag: Tag) extends Table[OsmWay](tag, "WAYS") {
    def id = column[Int]("id")
    def version = column[Int]("version")
    def user_id = column[Int]("user_id")
    def tstamp = column[Timestamp]("tstamp")
    def changeset_id = column[Int]("changeset_id")
    def tags = column[Map[String,String]]("tags")
    def nodes = column[List[Int]]("nodes")

    def * = (id, version, user_id, tstamp, changeset_id, tags, nodes) <> (OsmWay, OsmWay.unapply _)
  }
  val OsmWays = TableQuery[OsmWays]

  db withSession { implicit session: Session =>
    try {
      OsmWays.ddl create

      OsmWays.insert(OsmWay(101, 1, 111, new Timestamp(System.currentTimeMillis()), 1001, Map.empty, Nil))
      OsmWays.insert(OsmWay(102, 1, 111, new Timestamp(System.currentTimeMillis()), 1001, Map.empty, Nil))

      OsmWays.where(_.id === 101).map(w => (w.tags ~ w.nodes)).update(Map("t1"->"t1"), List(1))

      val waysWithT1 = OsmWays.where(w => w.tags @> Map("t1"->"t1").bind).map(w => w).list()
      waysWithT1.foreach(w => println(s"way(id:${w.id}, tags:${w.tags})"))
    } finally {
      OsmWays.ddl drop
    }
  }

}