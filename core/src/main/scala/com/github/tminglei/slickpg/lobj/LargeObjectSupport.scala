package com.github.tminglei.slickpg.lobj

import java.io.InputStream

import com.github.tminglei.slickpg.ExPostgresDriver
import org.postgresql.PGConnection
import org.postgresql.largeobject.LargeObjectManager

/**
  * Adds functionality for creating LargeObject upload actions
  */
trait LargeObjectSupport { driver: ExPostgresDriver =>

  import driver.api._

  /**
    * Builds an action for uploading Large Object instances to the database.
    * @param largeObjectStream The input stream containing the large object to upload.
    * @param bufferSize The number of bytes to process in each write loop.
    * @return A DBIO action which creates a Large Object in the database and returns the object's OID.
    */
  def buildLargeObjectUploadAction(largeObjectStream: InputStream, bufferSize: Int = 4096): SimpleDBIO[Long] = {
    SimpleDBIO { khan =>
      khan.connection.setAutoCommit(false)
      val largeObjectApi = khan.connection.unwrap(classOf[PGConnection]).getLargeObjectAPI
      val largeObjectId = largeObjectApi.createLO()
      val largeObject = largeObjectApi.open(largeObjectId, LargeObjectManager.WRITE)

      val bytes = new Array[Byte](bufferSize)

      Iterator.continually {
        val bytesRead = largeObjectStream.read(bytes)
        val bytesToWrite = if (bytesRead <= 0) {
          //nothing was read, so just return an empty byte array
          new Array[Byte](0)
        } else if (bytesRead < bufferSize) {
          //the read operation hit the end of the stream, so remove the unneeded cells
          val actualBytes = new Array[Byte](bytesRead)
          bytes.copyToArray(actualBytes)
          actualBytes
        } else {
          bytes
        }

        largeObject.write(bytesToWrite)
        bytesRead
      }.takeWhile { _ > 0 }.length //call .length to force evaluation
      largeObject.close()
      largeObjectId
    }
  }
}
