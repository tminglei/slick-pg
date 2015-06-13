import java.time.{LocalDateTime, LocalDate}

import com.github.tminglei.slickpg
import com.vividsolutions.jts.geom._
import play.api.data._
import play.api.data.format._
import java.util.UUID
import play.api.libs.json.JsValue

package object util {
  type Range[T] = slickpg.Range[T]
  val Range = slickpg.Range

  /// play form formatter aliases
  implicit val stringFormat = Formats.stringFormat
  implicit val longFormat = Formats.longFormat
  implicit val intFormat = Formats.intFormat
  implicit val booleanFormat = Formats.booleanFormat
  implicit val j8DateFormat = MyFormats.j8DateFormat
  implicit val j8DateTimeFormat = MyFormats.j8DateTimeFormat
  implicit val uuidFormat = MyFormats.uuidFormat
  implicit val intRangeFormat = MyFormats.rangeFormat[Int](_.toInt)
  implicit val longRangeFormat = MyFormats.rangeFormat[Long](_.toLong)
  implicit val floatRangeFormat = MyFormats.rangeFormat[Float](_.toFloat)
  implicit val dateRangeFormat = MyFormats.rangeFormat[LocalDate](LocalDate.parse)
  implicit val dateTimeRangeFormat = MyFormats.rangeFormat[LocalDateTime](LocalDateTime.parse)
  implicit val geometryFormat = MyFormats.geometryFormat[Geometry]
  implicit val pointFormat = MyFormats.geometryFormat[Point]
  implicit val polygonFormat = MyFormats.geometryFormat[Polygon]
  implicit val lineStringFormat = MyFormats.geometryFormat[LineString]
  implicit val linearRingFormat = MyFormats.geometryFormat[LinearRing]
  implicit val geometryCollectionFormat = MyFormats.geometryFormat[GeometryCollection]
  implicit val multiPointFormat = MyFormats.geometryFormat[MultiPoint]
  implicit val multiPolygonFormat = MyFormats.geometryFormat[MultiPolygon]
  implicit val multiLineStringFormat = MyFormats.geometryFormat[MultiLineString]
  implicit val strMapFormat = MyFormats.strMapFormat
  implicit val jsonFormat = MyFormats.jsonFormat

  // play form mappings
  val uuid: Mapping[UUID] = Forms.of[UUID]
  val point: Mapping[Point] = Forms.of[Point]
  val polygon: Mapping[Polygon] = Forms.of[Polygon]
  val date: Mapping[LocalDate] = Forms.of[LocalDate]
  val datetime: Mapping[LocalDateTime] = Forms.of[LocalDateTime]
  val strMap: Mapping[Map[String, String]] = Forms.of[Map[String, String]]
  val intRange: Mapping[Range[Int]] = Forms.of[Range[Int]]
  val longRange: Mapping[Range[Long]] = Forms.of[Range[Long]]
  val floatRange: Mapping[Range[Float]] = Forms.of[Range[Float]]
  val dateRange: Mapping[Range[LocalDate]] = Forms.of[Range[LocalDate]]
  val dateTimeRange: Mapping[Range[LocalDateTime]] = Forms.of[Range[LocalDateTime]]
  val json: Mapping[JsValue] = Forms.of[JsValue]
}
