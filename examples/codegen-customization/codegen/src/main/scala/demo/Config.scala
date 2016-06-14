package demo

object Config{
  val initScripts = Seq("drop-tables.sql","create-tables.sql","populate-tables.sql")
  // FIXME don't forget to adjust it according to your environment
  val url = "jdbc:postgresql://172.17.0.1/test?user=test"
  val jdbcDriver =  "org.postgresql.Driver"
  val slickProfile = MyPostgresDriver
}
