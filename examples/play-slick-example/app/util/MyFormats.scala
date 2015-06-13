package util

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import com.github.tminglei.slickpg.PgRangeSupportUtils
import play.api.data.format.Formats
import play.api.data.format.Formatter
import play.api.data.FormError
import com.vividsolutions.jts.io.{WKTReader, WKTWriter}
import com.vividsolutions.jts.geom.Geometry
import play.api.libs.json._

/**
 * my play form data formatters
 */
object MyFormats {

  def jsonFormat: Formatter[JsValue] = new Formatter[JsValue] {
    override val format = Some(("format.json", Nil))

    def bind(key: String, data: Map[String, String]) =
      parsing(Json.parse(_), "error.json", Nil)(key, data)
    def unbind(key: String, value: JsValue) = Map(key -> Json.stringify(value))
  }

  ///
  def j8DateFormat: Formatter[LocalDate] = new Formatter[LocalDate] {
    override val format = Some(("format.datetime", Nil))

    def bind(key: String, data: Map[String, String]) =
      parsing(LocalDate.parse, "error.datetime", Nil)(key, data)
    def unbind(key: String, value: LocalDate) = Map(key -> value.toString)
  }
  def j8DateTimeFormat: Formatter[LocalDateTime] = new Formatter[LocalDateTime] {
    override val format = Some(("format.datetime", Nil))

    def bind(key: String, data: Map[String, String]) =
      parsing(LocalDateTime.parse, "error.datetime", Nil)(key, data)
    def unbind(key: String, value: LocalDateTime) = Map(key -> value.toString)
  }

  ///
  def uuidFormat: Formatter[UUID] = new Formatter[UUID] {
    override val format = Some(("format.uuid", Nil))

    def bind(key: String, data: Map[String, String]) =
      parsing(UUID.fromString, "error.uuid", Nil)(key, data)
    def unbind(key: String, value: UUID) = Map(key -> value.toString)
  }

  def rangeFormat[T](parseFn: (String => T)): Formatter[Range[T]] = new Formatter[Range[T]] {
    override val format = Some(("format.range", Nil))

    def bind(key: String, data: Map[String, String]) =
      parsing(PgRangeSupportUtils.mkRangeFn(parseFn), "error.range", Nil)(key, data)
    def unbind(key: String, value: Range[T]) = Map(key -> value.toString)
  }

  ///
  def strMapFormat = new Formatter[Map[String, String]] {
    override val format = Some(("format.jsonmap", Seq("{key1:value1, key2:value2, ...}")))

    def bind(key: String, data: Map[String, String]) =
      parsing(fromJsonStr(_).getOrElse(Map.empty[String,String]), "error.jsonmap", Nil)(key, data)
    def unbind(key: String, value: Map[String,String]) = Map(key -> toJsonStr(value))
  }

  implicit private val mapReads = Reads.mapReads[String]
  implicit private val mapWrites = Writes.mapWrites[String]
  def toJsonStr(v: Map[String,String]): String = Json.stringify(Json.toJson(v))
  def fromJsonStr(s: String): Option[Map[String,String]] = Option(Json.fromJson(Json.parse(s)).get)
  
  ///
  def geometryFormat[T <: Geometry]: Formatter[T] = new Formatter[T] {
    override val format = Some(("format.geometry", Nil))

    def bind(key: String, data: Map[String, String]) =
      parsing(fromWKT[T], "error.geometry", Nil)(key, data)
    def unbind(key: String, value: T) = Map(key -> toWKT(value))
  }

  //////////////////////////////////////////////////////////////////////////
  private val wktWriterHolder = new ThreadLocal[WKTWriter]
  private val wktReaderHolder = new ThreadLocal[WKTReader]

  private def toWKT(geom: Geometry): String = {
    if (wktWriterHolder.get == null) wktWriterHolder.set(new WKTWriter())
    wktWriterHolder.get.write(geom)
  }
  private def fromWKT[T](wkt: String): T = {
    if (wktReaderHolder.get == null) wktReaderHolder.set(new WKTReader())
    wktReaderHolder.get.read(wkt).asInstanceOf[T]
  }

  /**
   * (copy from [[play.api.data.format.Formats#parsing]])
   * Helper for formatters binders
   * @param parse Function parsing a String value into a T value, throwing an exception in case of failure
   * @param errMsg Error to set in case of parsing failure
   * @param errArgs Arguments for error message
   * @param key Key name of the field to parse
   * @param data Field data
   */
  private def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Any])(
                         key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
    Formats.stringFormat.bind(key, data).right.flatMap { s =>
      scala.util.control.Exception.allCatch[T]
        .either(parse(s))
        .left.map(e => Seq(FormError(key, errMsg, errArgs)))
    }
  }
}
