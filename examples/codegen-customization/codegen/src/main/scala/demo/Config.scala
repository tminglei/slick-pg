package demo

object Config{
  //FIXME change it when necessary
  val pgHost = "192.168.99.100"
  //
  val initScripts = Seq("drop-tables.sql","create-tables.sql","populate-tables.sql")
  // FIXME don't forget to adjust it according to your environment
  val url = s"jdbc:postgresql://$pgHost:5432/test?user=test&password=test"
  val jdbcDriver =  "org.postgresql.Driver"
  val slickProfile: MyPostgresDriver = MyPostgresDriver
}
