package com.github.tminglei.slickpg
package utils

import org.junit._
import org.junit.Assert._
import scala.reflect.runtime.{universe => ru}

class ConverterUtilTest {
  import Converters.Util._
  import PGObjectTokenizer.PGElements._
  import ConverterUtilTest._

  @Test
  def testCacheKey(): Unit = {
    import Converters.CacheKey

    val tpe1 = ru.typeOf[T]
    val tpe2 = ru.typeOf[Element]

    assertEquals(CacheKey(tpe1, tpe2).hashCode(), CacheKey(tpe1, tpe2).hashCode())
    assertEquals(CacheKey(tpe1, tpe2), CacheKey(tpe1, tpe2))
  }

  @Test
  def testConverterUtil(): Unit = {
    Converters.register((v: String) => v.toInt)
    Converters.register((v: String) => v.toLong)
//    Converters.register((v: String) => v)

    // simple case
    val conv = mkConvFromElement[T]
    assertEquals(T(111,"test","test desc"),
      conv(CompositeE(List(ValueE("111"), ValueE("test"), ValueE("test desc"), ValueE(null)))))

    val conv1 = mkConvToElement[T]
    assertEquals(CompositeE(List(ValueE("112"), ValueE("test"), ValueE("test 2"), null)),
      conv1(T(112, "test", "test 2")))

    // simple nested case
    Converters.register(conv)
    val convt1 = mkConvFromElement[T1]
    assertEquals(T1(115,T(111,"test","test dd",Some("hi")),List(157)),
      convt1(CompositeE(List(ValueE("115"), CompositeE(List(ValueE("111"), ValueE("test"), ValueE("test dd"), ValueE("hi"))), ArrayE(List(ValueE("157")))))))

    Converters.register(conv1)
    val convt11 = mkConvToElement[T1]
    assertEquals(CompositeE(List(ValueE("116"), CompositeE(List(ValueE("111"), ValueE("test"), ValueE("test 3"), null)), ArrayE(List(ValueE("123"), ValueE("135"))))),
      convt11(T1(116, T(111, "test", "test 3"), List(123,135))))

    // composite array case
    Converters.register(convt1)
    val convat = mkArrayConvFromElement[T1]
    assertEquals(List(T1(115,T(111,"test","test dd",Some("hi")),List(157))),
      convat(ArrayE(List(CompositeE(List(ValueE("115"), CompositeE(List(ValueE("111"), ValueE("test"), ValueE("test dd"), ValueE("hi"))), ArrayE(List(ValueE("157")))))))))

    Converters.register(convt11)
    val convat1 = mkArrayConvToElement[T1]
    assertEquals(ArrayE(List(CompositeE(List(ValueE("115"), CompositeE(List(ValueE("111"), ValueE("test"), ValueE("test dd"), ValueE("hi"))), ArrayE(List(ValueE("157"))))))),
      convat1(List(T1(115,T(111,"test","test dd",Some("hi")),List(157)))))
  }
}

object ConverterUtilTest {
  // !!!NOTE: should use outer (static) classes, instead of inner (dynamic) classes
  case class T(id: Long, name: String, desc: String, opt: Option[String] = None)
  case class T1(id: Long, t: T, childIds: List[Long])
}