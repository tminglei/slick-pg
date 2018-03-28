package demo

object Config{
  val initScripts = Seq("drop-tables.sql","create-tables.sql","populate-tables.sql")
  // FIXME don't forget to adjust it according to your environment
  val url = "jdbc:postgresql://192.168.99.100:5432/test?user=test&password=test"
  val jdbcDriver =  "org.postgresql.Driver"
  val slickProfile: MyPostgresDriver = MyPostgresDriver
}
