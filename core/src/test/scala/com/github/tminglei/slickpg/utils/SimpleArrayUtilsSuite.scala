package com.github.tminglei.slickpg.utils

import org.scalatest.funsuite.AnyFunSuite

class SimpleArrayUtilsSuite extends AnyFunSuite {

  test("fromString") {
    val v1 = SimpleArrayUtils.fromString(identity)("""{)}""")
    assert(v1 === Some(Seq(")")))

    val v2 = SimpleArrayUtils.fromString(identity)("""{"prefix)"}""")
    assert(v2 === Some(Seq("prefix)")))

    val v3 = SimpleArrayUtils.fromString(identity)("""{")"}""")
    assert(v3 === Some(Seq(")")))

    val v4 = SimpleArrayUtils.fromString(identity)("""{"other",")"}""")
    assert(v4 === Some(Seq("other", ")")))

    val v5 = SimpleArrayUtils.fromString(identity)("""{")suffix"}""")
    assert(v5 === Some(Seq(")suffix")))
  }

  test("mkString & fromString") {
    val input = List("Test \\n")
    val str = SimpleArrayUtils.mkString[String](identity)(input)
    val result = SimpleArrayUtils.fromString[String](identity)(str).orNull
    assert(result == input)
  }
}
