package models

import java.time.LocalDate
import play.api.libs.json.JsValue

case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

case class Company(
  id: Option[Long],
  name: String,
  props: JsValue)

case class Computer(
  id: Option[Long] = None,
  name: String,
  introduced: Option[LocalDate] = None,
  discontinued: Option[LocalDate] = None,
  companyId: Option[Long] = None)