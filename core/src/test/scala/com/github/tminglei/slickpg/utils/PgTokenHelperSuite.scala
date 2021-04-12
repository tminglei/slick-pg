package com.github.tminglei.slickpg
package utils

import org.scalatest.funsuite.AnyFunSuite

class PgTokenHelperSuite extends AnyFunSuite {
  import PgTokenHelper._

  test("tokenize") {
    val input = """{"(201,\"(101,\"\"(test1'\"\",\"\"2001-01-03 13:21:00\"\",\"\"[\"\"\"\"2010-01-01 14:30:00\"\"\"\",\"\"\"\"2010-01-03 15:30:00\"\"\"\")\"\")\",t)"}"""
    val tokens = Tokenizer.tokenize(input)

    val expected = Open("{") +: Marker("\"") +: Open("(") +: Chunk("201") +: Comma +: Marker("\\\"") +: Open("(") +: Chunk("101") +: Comma +: Marker("\\\"\\\"") +: Open("(") +: Chunk("test1'") +: Marker("\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"") +: Chunk("2001-01-03 13:21:00") +: Marker("\\\"\\\"") +: Comma +: Marker("\\\"\\\"") +: Open("[") +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-01 14:30:00") +: Marker("\\\"\\\"\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-03 15:30:00") +: Marker("\\\"\\\"\\\"\\\"") +: Close(")") +: Marker("\\\"\\\"") +: Close(")") +: Marker("\\\"") +: Comma +: Chunk("t") +: Close(")") +: Marker("\"") +: Close("}") +: Nil

    assert(tokens === expected)
  }

  test("grouping") {
    val tokens = Open("{") +: Marker("\"") +: Open("(") +: Chunk("201") +: Comma +: Marker("\\\"") +: Open("(") +: Chunk("101") +: Comma +: Marker("\\\"\\\"") +: Open("(") +: Chunk("test1'") +: Marker("\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"") +: Chunk("2001-01-03 13:21:00") +: Marker("\\\"\\\"") +: Comma +: Marker("\\\"\\\"") +: Open("[") +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-01 14:30:00") +: Marker("\\\"\\\"\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-03 15:30:00") +: Marker("\\\"\\\"\\\"\\\"") +: Close(")") +: Marker("\\\"\\\"") +: Close(")") +: Marker("\\\"") +: Comma +: Chunk("t") +: Close(")") +: Marker("\"") +: Close("}") +: Nil
    val root = grouping(tokens)

    val expected =
      GroupToken(List(
        Marker(""),
        Open("{"),
        GroupToken(List(
          Marker("\""),
          Open("("),
          Chunk("201"),
          Comma,
          GroupToken(List(
            Marker("\\\""),
            Open("("),
            Chunk("101"),
            Comma,
            GroupToken(List(
              Marker("\\\"\\\""),
              Open("("),
              Chunk("test1'"),
              Marker("\\\"\\\"")
            )),
            Comma,
            GroupToken(List(
              Marker("\\\"\\\""),
              Chunk("2001-01-03 13:21:00"),
              Marker("\\\"\\\"")
            )),
            Comma,
            GroupToken(List(
              Marker("\\\"\\\""),
              Open("["),
              GroupToken(List(
                Marker("\\\"\\\"\\\"\\\""),
                Chunk("2010-01-01 14:30:00"),
                Marker("\\\"\\\"\\\"\\\"")
              )),
              Comma,
              GroupToken(List(
                Marker("\\\"\\\"\\\"\\\""),
                Chunk("2010-01-03 15:30:00"),
                Marker("\\\"\\\"\\\"\\\"")
              )),
              Close(")"),
              Marker("\\\"\\\"")
            )),
            Close(")"),
            Marker("\\\"")
          )),
          Comma,
          Chunk("t"),
          Close(")"),
          Marker("\"")
        )),
        Close("}"),
        Marker("")
      ))

    assert(root === expected)

    val root1 = grouping(Tokenizer.tokenize("""{"(\"(test1'\",,,)"}"""))
    val expected1 =
      GroupToken(List(
        Marker(""),
        Open("{"),
        GroupToken(List(
          Marker("\""),
          Open("("),
          GroupToken(List(
            Marker("\\\""),
            Open("("),
            Chunk("test1'"),
            Marker("\\\"")
          )),
          Comma,
          Null,
          Comma,
          Null,
          Comma,
          Null,
          Close(")"),
          Marker("\"")
        )),
        Close("}"),
        Marker("")
      ))
    assert(root1 === expected1)

    val root2 = grouping(Tokenizer.tokenize(("""{"(,102,,)"}""")))
    val expected2 =
      GroupToken(List(
        Marker(""),
        Open("{"),
        GroupToken(List(
          Marker("\""),
          Open("("),
          Null,
          Comma,
          Chunk("102"),
          Comma,
          Null,
          Comma,
          Null,
          Close(")"),
          Marker("\"")
        )),
        Close("}"),
        Marker("")
      ))
    assert(root2 === expected2)

    val root3 = grouping(Tokenizer.tokenize("""{"(,,,)"}"""))
    val expected3 =
      GroupToken(List(
        Marker(""),
        Open("{"),
        GroupToken(List(
          Marker("\""),
          Open("("),
          Null,
          Comma,
          Null,
          Comma,
          Null,
          Comma,
          Null,
          Close(")"),
          Marker("\"")
        )),
        Close("}"),
        Marker("")
      ))
    assert(root3 === expected3)
  }

  test("get string") {
    val input =
      GroupToken(List(
        Marker("\\\"\\\""),
        Open("["),
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-01 14:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        )),
        Comma,
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-03 15:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        )),
        Close(")"),
        Marker("\\\"\\\"")
      ))
    val rangeStr = getString(input, 2)

    val expected = """["2010-01-01 14:30:00","2010-01-03 15:30:00")"""
    assert(rangeStr === expected)
  }

  test("get children") {
    val input =
      GroupToken(List(
        Marker("\\\"\\\""),
        Open("["),
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-01 14:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        )),
        Comma,
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-03 15:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        )),
        Close(")"),
        Marker("\\\"\\\"")
      ))
    val children = getChildren(input)

    val expected =
      List(
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-01 14:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        )),
        GroupToken(List(
          Marker("\\\"\\\"\\\"\\\""),
          Chunk("2010-01-03 15:30:00"),
          Marker("\\\"\\\"\\\"\\\"")
        ))
      )

    assert(children === expected)
  }

  test("create string") {
    val input =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Marker("\""),
          Open("("),
          Chunk("201"),
          GroupToken(List(
            Marker("\\\""),
            Open("("),
            Chunk("101"),
            GroupToken(List(
              Marker("\\\"\\\""),
              Open("("),
              Chunk("test1'"),
              Marker("\\\"\\\"")
            )),
            GroupToken(List(
              Marker("\\\"\\\""),
              Chunk("2001-01-03 13:21:00"),
              Marker("\\\"\\\"")
            )),
            GroupToken(List(
              Marker("\\\"\\\""),
              Open("["),
              GroupToken(List(
                Marker("\\\"\\\"\\\"\\\""),
                Chunk("2010-01-01 14:30:00"),
                Marker("\\\"\\\"\\\"\\\"")
              )),
              GroupToken(List(
                Marker("\\\"\\\"\\\"\\\""),
                Chunk("2010-01-03 15:30:00"),
                Marker("\\\"\\\"\\\"\\\"")
              )),
              Close(")"),
              Marker("\\\"\\\"")
            )),
            Close(")"),
            Marker("\\\"")
          )),
          Chunk("t"),
          Close(")"),
          Marker("\"")
        )),
        Close("}")
      ))
    val pgStr = createString(input)

    val expected = """{"(201,\"(101,\"\"(test1'\"\",\"\"2001-01-03 13:21:00\"\",\"\"[\\\\\"\"2010-01-01 14:30:00\\\\\"\",\\\\\"\"2010-01-03 15:30:00\\\\\"\")\"\")\",t)"}"""

    assert(pgStr === expected)

    ///
    val input1 =
      GroupToken(List(
        Marker("\""),
        Open("("),
        Chunk("201"),
        GroupToken(List(
          Marker("\\\""),
          Open("("),
          Chunk("101"),
          GroupToken(List(
            Marker("\\\"\\\""),
            Open("("),
            Chunk("test1'"),
            Marker("\\\"\\\"")
          )),
          GroupToken(List(
            Marker("\\\"\\\""),
            Chunk("2001-01-03 13:21:00"),
            Marker("\\\"\\\"")
          )),
          GroupToken(List(
            Marker("\\\"\\\""),
            Open("["),
            GroupToken(List(
              Marker("\\\"\\\"\\\"\\\""),
              Chunk("2010-01-01 14:30:00"),
              Marker("\\\"\\\"\\\"\\\"")
            )),
            GroupToken(List(
              Marker("\\\"\\\"\\\"\\\""),
              Chunk("2010-01-03 15:30:00"),
              Marker("\\\"\\\"\\\"\\\"")
            )),
            Close(")"),
            Marker("\\\"\\\"")
          )),
          Close(")"),
          Marker("\\\"")
        )),
        Chunk("t"),
        Close(")"),
        Marker("\"")
      ))
    val pgStr1 = createString(input1)

    val expected1 = """(201,"(101,""(test1'"",""2001-01-03 13:21:00"",""[\\""2010-01-01 14:30:00\\"",\\""2010-01-03 15:30:00\\"")"")",t)"""
    assert(pgStr1 === expected1)

    ///
    val input2 =
      GroupToken(Open("{") +: List(
        GroupToken(Open("{") +: List(
          Chunk("11"),
          Chunk("12"),
          Chunk("13")
        ) :+ Close("}")),
        GroupToken(Open("{") +: List(
          Chunk("21"),
          Chunk("22"),
          Chunk("23")
        ) :+ Close("}"))
      ) :+ Close("}"))
    val pgStr2 = createString(input2)

    val expected2 = """{{11,12,13},{21,22,23}}"""
    assert(pgStr2 === expected2)
  }
}