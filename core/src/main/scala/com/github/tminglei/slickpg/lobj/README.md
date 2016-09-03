Large Objects
--------------
This project supports reading from and writing to Postgres' Large Object store via reactive streams.
Given a Large Object oid, you can stream a Large Object like so:
```scala
import com.github.tminglei.slickpg.lobj

val action = LargeObjectStreamingDBIOAction(
  largeObjectId = 1L,
  bufferSize = 16 * 1024 //You can set the default block size (in bytes) here. Setting it to 16 * 1024 will give us 16KB per read
)
val largeObjectStream: DatabasePublisher[Array[Byte]] = db.stream(action) //create the publishing stream on the object

//turn it into an Akka Stream Source for fun
val src = akka.stream.scaladsl.Source.fromPublisher(largeObjectStream)

//maybe turn it into a Scala Play chunked response if you're into that kind of thing
Ok.chunked(src).
  as("whatever/your-mimetype-is").
  withHeaders(
    "Content-Disposition" -> "attachment; filename=somefilename.txt"
  )
```

You can upload LargeObjects by wrapping them with an InputStream and then
running the appropriate DB action like so:
```scala

val inputStream: InputStream = _
val driver = new LargeObjectSupport with ExPostgresDriver {}
val action = driver.buildLargeObjectUploadAction(inputStream)
val largeObjectIdFuture: Future[Long] = db.run(action.transactionally)
```
