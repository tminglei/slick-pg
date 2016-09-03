package com.github.tminglei.slickpg.lobj

import java.io.InputStream

import org.postgresql.PGConnection
import org.postgresql.largeobject.LargeObjectManager
import slick.dbio.{Effect, Streaming, SynchronousDatabaseAction}
import slick.jdbc.JdbcBackend
import slick.util.DumpInfo

/**
  * Action for streaming Postgres LargeObject instances from a Postgres DB.
  * @param largeObjectId The oid of the LargeObject to stream.
  * @param bufferSize The chunk size in bytes. Default to 8KB.
  */
case class LargeObjectStreamingDBIOAction(largeObjectId: Long, bufferSize: Int = 1024 * 8) extends SynchronousDatabaseAction[Array[Byte], Streaming[Array[Byte]], JdbcBackend, Effect.All] {
  //our StreamState is the InputStream on the LargeObject instance and the number of bytes read in on the last run.
  type StreamState = (InputStream, Int)

  /**
    * Opens an InputStream on a Postgres LargeObject.
    * @param context The current database context.
    * @return An InputStream on a Postgres LargeObject.
    */
  private def openObject(context: JdbcBackend#Context): InputStream = {
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
  override def run(context: JdbcBackend#Context): Array[Byte] = throw new UnsupportedOperationException(s"Method 'run' is not supported for this action type.")

  override def getDumpInfo = DumpInfo(name = "LargeObjectStreamingDBIOAction")

  /**
    * Emits at most limit number of events to the context's stream.
    * @param context The current database context.
    * @param limit The maximum number of events to emit back to the stream.
    * @param state The state of the stream as returned by the previous iteration.
    * @return The new stream state.
    */
  override def emitStream(context: JdbcBackend#StreamingContext, limit: Long, state: StreamState): StreamState = {
    //open the stream iff no stream state exists
    val (stream, previousBytesRead) = state == null match {
      case true => (openObject(context), 1)
      case false => state
    }

    //read some byte arrays
    var count = 0L
    var bytesRead = previousBytesRead
    while (count < limit && bytesRead > 0) {
      val thing = readNextResult(stream)
      val bytes = thing._1
      bytesRead = thing._2
      context.emit(bytes)
      count += 1
    }

    //if the final bytesRead value was non-positive, close the stream and return a null StreamState
    //to indicate the end of this Stream
    bytesRead <= 0 match {
      case true =>
        stream.close()
        null
      case false => (stream, bytesRead)
    }
  }

  /**
    * Cancels this stream and closes the underlying InputStream.
    * @param context The current database context.
    * @param state The current StreamState at the time of the cancelling.
    */
  override def cancelStream(context: JdbcBackend#StreamingContext, state: StreamState): Unit = {
    if (state != null) {
      val (stream, _) = state
      stream.close()
    }
  }
}

