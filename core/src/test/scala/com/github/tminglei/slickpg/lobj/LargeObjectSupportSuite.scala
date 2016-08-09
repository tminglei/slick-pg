package com.github.tminglei.slickpg.lobj

import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

import com.github.tminglei.slickpg.{ExPostgresDriver, utils}
import org.scalatest.FunSuite

import scala.concurrent.{Await, ExecutionContext}
import scala.util.Success
import scala.concurrent.duration._

class LargeObjectSupportSuite extends FunSuite {
  implicit val testExecContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))
  import ExPostgresDriver.api._

  val driver = new LargeObjectSupport with ExPostgresDriver {}

  val db = Database.forURL(url = utils.dbUrl, driver = "org.postgresql.Driver")

  test("upload and download large object") {
    val testString = "some string to store as a large object"
    val largeObjectUploadStream = new ByteArrayInputStream(testString.getBytes)
    val uploadAction = driver.buildLargeObjectUploadAction(largeObjectUploadStream)
    val composedAction = uploadAction.flatMap(oid => LargeObjectStreamingDBIOAction(oid))
    val dbPublisher = db.stream(composedAction)

    val messageBuffer: StringBuffer = new StringBuffer()
    val f = dbPublisher.foreach(bytes => messageBuffer.append(new String(bytes))).andThen {
      case t: Success => assert(messageBuffer.toString == testString)
    }

    Await.result(f, 60.seconds)
  }
}
