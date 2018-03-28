package demo

import Config._
import slick.sql.SqlProfile.ColumnOption

import scala.concurrent.duration._
import scala.concurrent.Await

/**
 *  This customizes the Slick code generator. We only do simple name mappings.
 *  For a more advanced example see https://github.com/cvogt/slick-presentation/tree/scala-exchange-2013
 */
object CustomizedCodeGenerator {
  import scala.concurrent.ExecutionContext.Implicits.global

  val projectDir = System.getProperty("user.dir")

  def main(args: Array[String]): Unit = {
    // prepare database
    for(script <- initScripts) {
      // FIXME don't forget to adjust it according to your environment
      val cmd = s"psql -U test -h 192.168.99.100 -p 5432 -d test -f $projectDir/src/sql/$script"
      val exec = Runtime.getRuntime.exec(cmd)
      if (exec.waitFor() == 0) {
        println(s"$script finished.")
      }
    }

    // write the generated results to file
    Await.result(
      codegen.map(_.writeToFile(
        "demo.MyPostgresDriver", // use our customized postgres driver
        s"$projectDir/target/scala-2.12/src_managed/slick",
        "demo",
        "Tables",
        "Tables.scala"
      )),
      20.seconds
    )
  }

  val db = slickProfile.api.Database.forURL(url,driver=jdbcDriver)

  // filter out desired tables
  val included = Seq("COFFEE","SUPPLIER","COFFEE_INVENTORY")
  lazy val codegen = db.run {
    slickProfile.defaultTables.map(_.filter(t => included contains t.name.name.toUpperCase))
      .flatMap( slickProfile.createModelBuilder(_, ignoreInvalidDefaults = false).buildModel )
  }.map { model =>
    new slick.codegen.SourceCodeGenerator(model) {
      override def Table = new Table(_) { table =>
        override def Column = new Column(_) { column =>
          // customize db type -> scala type mapping, pls adjust it according to your environment
          override def rawType: String = {
            model.options.find(_.isInstanceOf[ColumnOption.SqlType]).flatMap {
              tpe =>
                tpe.asInstanceOf[ColumnOption.SqlType].typeName match {
                  case "hstore" => Option("Map[String, String]")
                  case "_text"|"_varchar" => Option("List[String]")
                  case "geometry" => Option("com.vividsolutions.jts.geom.Geometry")
                  case "_int8" => Option("List[Long]")
                  case "_int4" => Option("List[Int]")
                  case "_int2" => Option("List[Short]")
                  case _ =>       None
                }
            }.getOrElse{
                model.tpe match {
                  case "java.sql.Date" => "org.joda.time.LocalDate"
                  case "java.sql.Time" => "org.joda.time.LocalTime"
                  case "java.sql.Timestamp" => "org.joda.time.LocalDateTime"
                  case _ =>
                    super.rawType

            }}
          }
        }
      }

      // ensure to use our customized postgres driver at `import profile.simple._`
      override def packageCode(profile: String, pkg: String, container: String, parentType: Option[String]) : String = {
        s"""
package ${pkg}
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object ${container} extends {
  val profile = ${profile}
} with ${container}

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait ${container}${parentType.map(t => s" extends $t").getOrElse("")} {
  val profile: $profile
  import profile.api._
  ${indent(code)}
}
      """.trim()
      }
    }
  }
}