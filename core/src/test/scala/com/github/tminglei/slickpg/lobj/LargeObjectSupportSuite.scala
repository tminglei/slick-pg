package com.github.tminglei.slickpg
package lobj

import java.io.ByteArrayInputStream

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.Await
import scala.util.{Failure, Success}
import scala.concurrent.duration._

class LargeObjectSupportSuite extends AnyFunSuite with PostgresContainer {
  import ExPostgresProfile.api._

  val driver = new LargeObjectSupport with ExPostgresProfile {}

  lazy val db = Database.forURL(url = container.jdbcUrl, driver = "org.postgresql.Driver")

  test("upload and download large object") {
    val testString = "some string to store as a large object"
    val largeObjectUploadStream = new ByteArrayInputStream(testString.getBytes)
    val uploadAction = driver.buildLargeObjectUploadAction(largeObjectUploadStream)
    val composedAction = uploadAction.flatMap(oid => LargeObjectStreamingDBIOAction(oid))
    val dbPublisher = db.stream(composedAction.transactionally)

    val messageBuffer: StringBuffer = new StringBuffer()
    val f = dbPublisher.foreach(bytes => messageBuffer.append(new String(bytes))).andThen {
      case t: Success[Unit] => assert(messageBuffer.toString == testString)
      case Failure(error) => throw error
    }

    Await.result(f, 60.seconds)
  }
}
