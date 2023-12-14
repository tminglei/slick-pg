package com.github.tminglei.slickpg

import com.github.tminglei.slickpg.PgCompositeSupportSuite.ts
import org.postgresql.util.HStoreConverter

import java.time.LocalDateTime
import scala.jdk.CollectionConverters._

object ScalaVersionShim {
  def mapToString(m: Map[String, String]): String = HStoreConverter.toString((m).asJava)
  def stringToMap(s: String): Map[String, String] = (HStoreConverter.fromString(s)
    .asInstanceOf[java.util.Map[String, String]]).asScala.toMap

  def registerTypeConverters(): Unit = {

    utils.TypeConverters.register(PgRangeSupportUtils.mkRangeFn(ts))
    utils.TypeConverters.register(PgRangeSupportUtils.toStringFn[LocalDateTime](_.toString))
    utils.TypeConverters.register(mapToString)
    utils.TypeConverters.register(stringToMap)
  }

  object maybeTypeConverters {}
}

object TypeConverters {}
