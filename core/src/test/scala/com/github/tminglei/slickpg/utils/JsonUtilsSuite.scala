package com.github.tminglei.slickpg.utils

import org.scalatest.FunSuite

class JsonUtilsSuite extends FunSuite {
  import JsonUtils._

  test("clean") {
    val input1 = """123\u000045\\u00006\\\u00007"""
    val expected1 = """12345\\u00006\\7"""
    assert(clean(input1) === expected1)

    // This byte string equal to {"d":"123\u000045\u00006"}
    val unicodeJsonBytes: List[Byte] = List(123, 34, 100, 34, 58, 34, 49, 50, 51, 92, 117, 48, 48, 48, 48, 52, 53, 92, 117, 48, 48, 48, 48, 54, 34, 125)
    val input2 = new String(unicodeJsonBytes.map(_.toChar).toArray)
    val expected2 = """{"d":"123456"}"""
    assert(clean(input2) === expected2)

    val input3 = """123\u000045\\u00006\\\\u00007\\\\\u00008"""
    val expected3 = """12345\\u00006\\\\u00007\\\\8"""
    assert(clean(input3) === expected3)
  }
}
