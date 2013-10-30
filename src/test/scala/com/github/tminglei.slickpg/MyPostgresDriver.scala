package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import java.sql.{Date, Timestamp}

trait MyPostgresDriver extends PostgresDriver
                          with PgArraySupport
                          with PgDatetimeSupport
                          with PgRangeSupport
                          with PgHStoreSupport
                          with PgJsonSupport[text.Document]
                          with PgSearchSupport
                          with PostGISSupport {

  type RangeType[T] = Range[T]

  private val tsFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  private val dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd")
  private def toTimestamp(str: String) = new Timestamp(tsFormatter.parse(str).getTime)
  private def toSQLDate(str: String) = new Date(dateFormatter.parse(str).getTime)


  override val jsonMethods = org.json4s.native.JsonMethods

  override val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
                        with ArrayImplicits
                        with DatetimeImplicits
                        with RangeImplicits
                        with HStoreImplicits
                        with JsonImplicits
                        with SearchImplicits
                        with PostGISImplicits {

    // range type mapper declarations
    implicit val intRangeTypeMapper = new RangeTypeMapper(classOf[Int], Range.mkParser(_.toInt))
    implicit val longRangeTypeMapper = new RangeTypeMapper(classOf[Long], Range.mkParser(_.toLong))
    implicit val floatRangeTypeMapper = new RangeTypeMapper(classOf[Float], Range.mkParser(_.toFloat))
    implicit val timestampRangeTypeMapper = new RangeTypeMapper(classOf[Timestamp], Range.mkParser(toTimestamp))
    implicit val dateRangeTypeMapper = new RangeTypeMapper(classOf[Date], Range.mkParser(toSQLDate))

  }

  trait SimpleQLPlus extends SimpleQL
                        with ImplicitsPlus
                        with SearchAssistants
                        with PostGISAssistants
}

object MyPostgresDriver extends MyPostgresDriver
