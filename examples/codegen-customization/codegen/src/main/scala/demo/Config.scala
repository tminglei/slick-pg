package demo

object Config{
  // connection info for a pre-populated throw-away, in-memory db for this demo, which is freshly initialized on every run
  val initScripts = Seq("drop-tables.sql","create-tables.sql","populate-tables.sql")
  val url = "jdbc:postgresql://localhost/test?user=test"
  val jdbcDriver =  "org.postgresql.Driver"
  val slickProfile = MyPostgresDriver
}
