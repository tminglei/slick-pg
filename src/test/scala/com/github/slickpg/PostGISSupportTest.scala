package com.github.slickpg

import org.junit._
import org.junit.Assert._

class PostGISSupportTest {
  import MyPostgresDriver.simple._

  val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")


}
