Slick-pg
========
[Slick](https://github.com/slick/slick "Slick") extensions for PostgreSQL, to support a series of pg data types and related operators/functions.

####Currently supported pg types:
- ARRAY
- Date/Time
- Range
- Hstore
- JSON
- `text` Search
- `postgis` Geometry

** _tested on `postgreSQL 9.3` with `Slick 1.0.1`._

Install
-------
To use `slick-pg` in [sbt](http://www.scala-sbt.org/ "slick-sbt") project, add the following to your project file:
```scala
libraryDependencies += "com.github.tminglei" % "slick-pg_2.10.1" % "0.2.2.2"
```

Or, in [maven](http://maven.apache.org/ "maven") project, you can add `slick-pg` to your `pom.xml` like this:
```xml
<dependency>
    <groupId>com.github.tminglei</groupId>
    <artifactId>slick-pg_2.10.1</artifactId>
    <version>0.2.2.2</version>
</dependency>
```

Usage
------
Before using it, you need integrate it with PostgresDriver maybe like this:
```scala
import slick.driver.PostgresDriver
import com.github.tminglei.slickpg._

trait MyPostgresDriver extends PostgresDriver
                          with PgArraySupport
                          with PgDateSupport
                          with PgRangeSupport
                          with PgJsonSupport
                          with PgHStoreSupport
                          with PgSearchSupport
                          with PgPostGISSupport {
  /// for json support
  type DOCType = text.Document
  override val jsonMethods = org.json4s.native.JsonMethods

  ///
  override val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
                        with ArrayImplicits
                        with DateTimeImplicits
                        with RangeImplicits
                        with HStoreImplicits
                        with JsonImplicits
                        with SearchImplicits
                        with PostGISImplicits

  trait SimpleQLPlus extends SimpleQL
                        with ImplicitsPlus
                        with SearchAssistants
}

object MyPostgresDriver extends MyPostgresDriver

```

then in your codes you can use it like this:
```scala
import MyPostgresDriver.simple._

object TestTable extends Table[Test](Some("xxx"), "Test") {
  def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
  def during = column[Range[Timestamp]]("during")
  def location = column[Point]("location")
  def text = column[String]("text", O.DBType("varchar(4000)"))
  def props = column[Map[String,String]]("props_hstore")
  def tags = column[List[String]]("tags_arr")

  def * = id ~ during ~ location ~ text ~ props ~ tags <> (Test, Test unapply _)

  ///
  def byId(ids: Long*) = TestTable.where(_.id inSetBind ids).map(t => t)
  // will generate sql like: select * from test where tags && ?
  def byTag(tags: String*) = TestTable.where(_.tags @& tags.toList.bind).map(t => t)
  // will generate sql like: select * from test where during && ?
  def byTsRange(tsRange: Range[Timestamp]) = TestTable.where(_.during @& tsRange.bind).map(t => t)
  // will generate sql like: select * from test where case(props -> ? as [T]) == ?
  def byProperty[T](key: String, value: T) = TestTable.where(_.props.>>[T](key.bind) === value.bind).map(t => t)
  // will generate sql like: select * from test where ST_DWithin(location, ?, ?)
  def byDistance(point: Point, distance: Int) = TestTable.where(r => r.location.dWithin(point.bind, distance.bind)).map(t => t)
  // will generate sql like: select id, text, ts_rank(to_tsvector(text), to_tsquery(?)) from test where to_tsvector(text) @@ to_tsquery(?) order by ts_rank(to_tsvector(text), to_tsquery(?))
  def search(queryStr: String) = TestTable.where(tsVector(_.text) @@ tsQuery(queryStr.bind)).map(r => (r.id, r.text, tsRank(tsVector(r.text), tsQuery(queryStr.bind)))).sortBy(_._3)
}

...
 
```

Configurable type/mappers
-------------------------
Since v0.2.0, `slick-pg` started to support configurable type/mappers.

Here's the related technical details:
> All pg type oper/functions related codes and some core type mapper logics were extracted to a new sub project "slick-pg_core", and the oper/functions and type/mappers binding related codes were retained in the main project "slick-pg".

**So, if you need bind different scala type/mappers to a pg type oper/functions, you can do it as "slick-pg" currently did.**

** _ps: existing app codes won't be impacted._

####Built in supported type/mappers:
|          scala Type                 |        pg Type        |
| ----------------------------------- | --------------------- |
| List[T]                             | Pg ARRAY              |
| sql.Date/Time/Timestamp + tminglei.slickpg.Interval | Pg Date/Time |
| jada.time.LocalDate/LocalTime/LocalDateTime/Period  | Pg Date/Time |
| threeten.bp.LocalDate/LocalTime/LocalDateTime/Duration | Pg Date/Time |
| tminglei.slickpg.Range[T]           | Pg Range              |
| Map[String,String]                  | Pg HStore             |
| json4s.JValue                       | Pg JSON               |
| (TsQuery+TsVector)                  | Pg `text` Search      |
| jts.geom.Geometry                   | Pg `postgis` Geometry |


Build instructions
------------------
`slick-pg` uses sbt for building. Assume you have already installed sbt, then you can simply clone the git repository and build `slick-pg` in the following way:
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
val db = Database.forURL(url = "jdbc:postgresql://localhost/test?user=test", driver = "org.postgresql.Driver")
```


Support details
------------------------------
- Array's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/array "Array's oper/functions")
- JSON's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/json "JSON's oper/functions")
- Date/Time's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/date "Date/Time's oper/functions")
- Range's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/range "Range's oper/functions")
- HStore's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/hstore "HStore's oper/functions")
- Search's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/search "Search's oper/functions")
- Geometry's [oper/functions](https://github.com/tminglei/slick-pg/tree/master/core/src/main/scala/com/github/tminglei/slickpg/geom "Geometry's oper/functions")
- `basic` Composite type [support](https://github.com/tminglei/slick-pg/tree/slick1/core/src/main/scala/com/github/tminglei/slickpg/composite "Composite type Support")


Version history
---------------
v0.2.5 (10-Jan-2014):  
1) add basic composite type support  
2) array support: allow nested composite type

0.2.2 (04-Nov-2013):  
1) support Joda date/time, binding to Pg Date/Time  
2) support threetenbp date/time, binding to Pg Date/Time

v0.2.0 (01-Nov-2013):  
1) re-arch to support configurable type/mappers

v0.1.5 (29-Sep-2013):  
1) support pg json

v0.1.2 (31-Jul-2013):  
1) add pg datetime support  

v0.1.0 (20-May-2013):  
1) support pg array  
2) support pg range  
3) support pg hstore  
4) support pg search  
5) support pg geometry  


Existing issues
----------------
1) When using `slick-pg`'s uuid array support, you maybe encountered an exception said like 'Method `Jdbc4Array.getArrayImpl(long,int,Map)` is not yet implemented'.
That's because uuid array is not supported by postgres jdbc driver yet.  
I have submitted enhancement changes to postgres jdbc driver's development team, pls see [add uuid array support](https://github.com/pgjdbc/pgjdbc/pull/81 "add uuid array support") for details.


License
-------
Licensing conditions (BSD-style) can be found in LICENSE.txt.
