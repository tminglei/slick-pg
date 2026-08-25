package com.github.tminglei.slickpg
package lobj

import java.io.InputStream

import org.postgresql.PGConnection
import org.postgresql.largeobject.LargeObjectManager
import slick.dbio.{Effect, Streaming, SynchronousDatabaseAction}
import slick.jdbc.JdbcBackend
import slick.util.{CloseableIterator, DumpInfo}

/**
  * Action for streaming Postgres LargeObject instances from a Postgres DB.
  * @param largeObjectId The oid of the LargeObject to stream.
  * @param bufferSize The chunk size in bytes. Default to 8KB.
  */
case class LargeObjectStreamingDBIOAction(largeObjectId: Long, bufferSize: Int = 1024 * 8)
  extends SynchronousDatabaseAction[
    Array[Byte],
    Streaming[Array[Byte]],
    JdbcBackend#JdbcActionContext,
    Effect.All
  ] {


  /**
    * Opens an InputStream on a Postgres LargeObject.
    * @param context The current database context.
    * @return An InputStream on a Postgres LargeObject.
    */
  private def openObject(context: JdbcBackend#JdbcActionContext): InputStream = {
    context.connection.setAutoCommit(false)
    val largeObjectApi = context.connection.unwrap(classOf[PGConnection]).getLargeObjectAPI
    val largeObject = largeObjectApi.open(largeObjectId, LargeObjectManager.READ, false)
    largeObject.getInputStream
  }

  /**
    * Reads the next result from the InputStream as an Array of Bytes.
    * @param stream The current LargeObject InputStream.
    * @return A tuple containing the next chunk of bytes, and an integer indicating the number of bytes read.
    */
  private def readNextResult(stream: InputStream): (Array[Byte], Int) = {
    val bytes = new Array[Byte](bufferSize)
    val bytesRead = stream.read(bytes)
    if (bytesRead <= 0) {
      //nothing was read, so just return an empty byte array
      (new Array[Byte](0), bytesRead)
    } else if (bytesRead < bufferSize) {
      //the read operation hit the end of the stream, so remove the unneeded cells
      val actualBytes = new Array[Byte](bytesRead)
      bytes.copyToArray(actualBytes)
      (actualBytes, bytesRead)
    } else {
      (bytes, bytesRead)
    }
  }

  /**
    * Run this action. This is currently unsupported as this action only works for streaming and will throw
    * an UnsupportedOperationException.
    * @param context The current database context.
    * @return An UnsupportedOperationException with a friendly message.
    */
  override def run(context: JdbcBackend#JdbcActionContext): Array[Byte] = throw new UnsupportedOperationException(s"Method 'run' is not supported for this action type.")

  override def getDumpInfo = DumpInfo(name = "LargeObjectStreamingDBIOAction")

  override def openStream(ctx: JdbcBackend#JdbcActionContext): CloseableIterator[Array[Byte]] =
    CloseableIterator.close(openObject(ctx)).after { stream =>
      var done = false
      val iter = new Iterator[Array[Byte]] {
        def hasNext = !done
        def next() = {
          val (bytes, bytesRead) = readNextResult(stream)
          if (bytesRead <= 0) { done = true; new Array[Byte](0) }
          else bytes
        }
      }.filter(_.nonEmpty)
      CloseableIterator.wrap(iter)
    }
}
