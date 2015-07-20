Slick-pg
========
[![Build Status](https://travis-ci.org/tminglei/slick-pg.svg?branch=master)](https://travis-ci.org/tminglei/slick-pg)

[Slick](https://github.com/slick/slick "Slick") extensions for PostgreSQL, to support a series of pg data types and related operators/functions.

#### Currently supported pg types:
- ARRAY
- Date/Time
- Enum
- Range
- Hstore
- LTree
- JSON
- Inet/MacAddr
- `text` Search
- `postgis` Geometry

#### Currently supported pg features:
- inherits
- composite type (`basic`)


** _tested on `PostgreSQL` `v9.4` with `Slick` `v3.0.0`._


Usage
------
Before using it, you need integrate it with PostgresDriver maybe like this:
```scala
import com.github.tminglei.slickpg._

trait MyPostgresDriver extends ExPostgresDriver
                          with PgArraySupport
                          with PgDateSupport
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgPlayJsonSupport
                          with PgSearchSupport
                          with PgPostGISSupport 
                          with PgNetSupport 
                          with PgLTreeSupport {
  override val pgjson = "jsonb" //to keep back compatibility, pgjson's value was "json" by default

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
                           with DateTimeImplicits
                           with JsonImplicits
                           with NetImplicits
                           with LTreeImplicits
                           with RangeImplicits
                           with HStoreImplicits
                           with SearchImplicits
                           with SearchAssistants {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val playJsonArrayTypeMapper =
      new AdvancedArrayJdbcType[JsValue](pgjson,
        (s) => utils.SimpleArrayUtils.fromString[JsValue](Json.parse(_))(s).orNull,
        (v) => utils.SimpleArrayUtils.mkString[JsValue](_.toString())(v)
      ).to(_.toList)
  }
}

object MyPostgresDriver extends MyPostgresDriver

```

then in your codes you can use it like this:
```scala
import MyPostgresDriver.api._

class TestTable(tag: Tag) extends Table[Test](tag, Some("xxx"), "Test") {
  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
  def during = column[Range[Timestamp]]("during")
  def location = column[Point]("location")
  def text = column[String]("text", O.DBType("varchar(4000)"))
  def props = column[Map[String,String]]("props_hstore")
  def tags = column[List[String]]("tags_arr")

  def * = (id, during, location, text, props, tags) <> (Test.tupled, Test.unapply)
}

object tests extends TableQuery(new TestTable(_)) {
  // will generate sql like: 
  //   select * from test where id = ?
  def byId(ids: Long*) = tests
        .filter(_.id inSetBind ids)
        .map(t => t)
  // will generate sql like: 
  //   select * from test where tags && ?
  def byTag(tags: String*) = tests
        .filter(_.tags @& tags.toList.bind)
        .map(t => t)
  // will generate sql like: 
  //   select * from test where during && ?
  def byTsRange(tsRange: Range[Timestamp]) = tests
        .filter(_.during @& tsRange.bind)
        .map(t => t)
  // will generate sql like: 
  //   select * from test where case(props -> ? as [T]) == ?
  def byProperty[T](key: String, value: T) = tests
        .filter(_.props.>>[T](key.bind) === value.bind)
        .map(t => t)
  // will generate sql like: 
  //   select * from test where ST_DWithin(location, ?, ?)
  def byDistance(point: Point, distance: Int) = tests
        .filter(r => r.location.dWithin(point.bind, distance.bind))
        .map(t => t)
  // will generate sql like: 
  //   select id, text, ts_rank(to_tsvector(text), to_tsquery(?)) 
  //   from test where to_tsvector(text) @@ to_tsquery(?) 
  //   order by ts_rank(to_tsvector(text), to_tsquery(?))
  def search(queryStr: String) = tests
        .filter( t => {tsVector(t.text) @@ tsQuery(queryStr.bind)})
        .map(r => (r.id, r.text, tsRank(tsVector(r.text), tsQuery(queryStr.bind))))
        .sortBy(_._3)
}

...
```

_p.s. above codes are for `Slick` Lifted Embedding SQL. Except that, `slick-pg` also support for `Slick` Plain SQL, for details and usages pls refer to source codes and tests._

Install
-------
To use `slick-pg` in [sbt](http://www.scala-sbt.org/ "slick-sbt") project, add the following to your project file:
```scala
libraryDependencies += "com.github.tminglei" %% "slick-pg" % "0.9.1"
```


Or, in [maven](http://maven.apache.org/ "maven") project, you can add `slick-pg` to your `pom.xml` like this:
```xml
<dependency>
    <groupId>com.github.tminglei</groupId>
    <artifactId>slick-pg_2.11</artifactId>
    <version>0.9.1</version>
</dependency>
```

**For other related 3rd party dependencies, pls manually add them to your project.**  
> _Note: from `slick-pg` v0.7.0, I declared 3rd party dependencies as `provided`, and merged all of the plugin codes into main package. So you needn't add these **plugins** any more, but for these used 3rd party dependencies, you're still required to add them to your project manually._


Configurable type/mappers
-------------------------
Since v0.2.0, `slick-pg` started to support configurable type/mappers.

Here's the related technical details:
> All pg type oper/functions related codes and some core type mapper logics were extracted to a new sub project "slick-pg_core", and the oper/functions and type/mappers binding related codes were retained in the main project "slick-pg".

**So, if you need bind different scala type/mappers to a pg type oper/functions, you can do it as "slick-pg" currently did.**


####Built in supported type/mappers:
|          scala Type                 |        pg Type        |    dev 3rd-party library dependency    |
| ----------------------------------- | --------------------- | -------------------------------------- |
| List[T]                             | ARRAY                 |        no 3rd party dependencies       |
| `sql` Date<br> Time<br> Timestamp<br> slickpg Interval<br> Calendar | date<br> time<br> timestamp<br> interval<br> timestamptz |    no 3rd party dependencies     |
| `joda` LocalDate<br> LocalTime<br> LocalDateTime<br> Period<br> DateTime  | date<br> time<br> timestamp<br> interval<br> timestamptz |    `joda-time` v2.4 / `joda-convert` v1.7     |
| `java.time` LocalDate<br> LocalTime<br> LocalDateTime<br> Duration<br> ZonedDateTime | date<br> time<br> timestamp<br> interval<br> timestamptz |    no 3rd party dependencies <br> but require java 8    |
| `threeten.bp` LocalDate<br> LocalTime<br> LocalDateTime<br> Duration<br> ZonedDateTime | date<br> time<br> timestamp<br> interval<br> timestamptz |    `threetenbp` v1.0      |
| `scala` Enumeration                 | enum                  |        no 3rd party dependencies       |
| `slickpg` Range[T]                  | range                 |        no 3rd party dependencies       |
| `slickpg` LTree                     | ltree                 |        no 3rd party dependencies       |
| Map[String,String]                  | hstore                |        no 3rd party dependencies       |
| `slickpg` InetString                | inet                  |        no 3rd party dependencies       |
| `slickpg` MacAddrString             | macaddr               |        no 3rd party dependencies       |
| `slickpg` JsonString                | json                  |        no 3rd party dependencies       |
| `json4s` JValue                     | json                  |        `json4s` v3.2.10                |
| `play-json` JsValue                 | json                  |        `play-json` v2.3.0              |
| `spray-json` JsValue                | json                  |        `spray-json` v1.3.1             |
| `argonaut json` Json                | json                  |        `argonaut` v6.0.4               |
| (TsQuery+TsVector)                  | `text` search         |        no 3rd party dependencies       |
| `jts` Geometry                      | `postgis` geometry    |        `jts` v1.13                     |


Build instructions
------------------
`slick-pg` uses SBT for building and requires Java 8, since it provides support for `java.date` in addon `date2`. Assume you have already installed SBT, then you can simply clone the git repository and build `slick-pg` in the following way:
```
./sbt update
./sbt compile
```

_To run the test suite, you need:_
- create a user 'test' and db 'test' on your local postgres server, and 
- the user 'test' should be an super user and be the owner of db 'test'

Then you can run the tests like this:
```
./sbt test
```
_ps: in the code of unit tests, the `slick` database is setup like this:_
```scala
val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=postgres", driver = "org.postgresql.Driver")
```


Details
------------------------------
- Array's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/array "Array's oper/functions"), usage  [cases](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/PgArraySupportSuite.scala "test cases")
- JSON's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/json "JSON's oper/functions"), usage cases for [json4s](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/addon/PgJson4sSupportSuite.scala "test cases"), [play-json](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/addon/PgPlayJsonSupportSuite.scala "test cases"), [spray-json](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/addon/PgPlayJsonSupportSuite.scala "test cases") and [argonaut json](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/addon/PgArgonautSupportSuite.scala "test cases")
- Date/Time's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/date "Date/Time's oper/functions"), usage cases for [java date](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/PgDateSupportSuite.scala "test cases"), [joda time](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/addon/PgDateSupportJodaSuite.scala "test cases"), and [java 8 date](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/addon/PgDate2SupportSuite.scala "test cases") and [threeten bp](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/addon/PgDate2bpSupportSuite.scala "test cases")
- Enum's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/enums "Enum's oper/functions"), usage [cases](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/PgEnumSupportSuite.scala "test cases")
- Range's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/range "Range's oper/functions"), usage [cases](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/PgRangeSupportSuite.scala "test cases")
- HStore's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/hstore "HStore's oper/functions"), usage [cases](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/PgHStoreSupportSuite.scala "test cases")
- LTree's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/ltree "LTree's oper/functions"), usage [cases](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/PgLTreeSupportSuite.scala "test cases")
- Inet/MacAddr's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/net "net's oper/functions"), usage [cases](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/PgNetSupportSuite.scala "test cases")
- Search's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/search "Search's oper/functions"), usage [cases](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/PgSearchSupportSuite.scala "test cases")
- Geometry's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/geom "Geometry's oper/functions"), usage cases for [postgis](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/addon/PgPostGISSupportSuite.scala "test cases")
- `basic` Composite type [support](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/composite "Composite type Support"), usage [cases](https://github.com/tminglei/slick-pg/blob/master/src/test/scala/com/github/tminglei/slickpg/PgCompositeSupportSuite.scala "test cases")


License
-------
Licensing conditions (BSD-style) can be found in LICENSE.txt.
