package com.github.tminglei.slickpg
package utils

import org.scalatest.FunSuite

class PgTokenHelperSuite extends FunSuite {
  import PgTokenHelper._

  test("tokenize") {
    val input = """{"(201,\"(101,\"\"(test1'\"\",\"\"2001-01-03 13:21:00\"\",\"\"[\"\"\"\"2010-01-01 14:30:00\"\"\"\",\"\"\"\"2010-01-03 15:30:00\"\"\"\")\"\")\",t)"}"""
    val tokens = Tokenizer.tokenize(input)

    val expected = Open("{") +: Open("(","\"") +: Chunk("201") +: Comma +: Open("(","\\\"") +: Chunk("101") +: Comma +: Open("(","\\\"\\\"") +: Chunk("test1'") +: Marker("\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"") +: Chunk("2001-01-03 13:21:00") +: Marker("\\\"\\\"") +: Comma +: Open("[","\\\"\\\"") +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-01 14:30:00") +: Marker("\\\"\\\"\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-03 15:30:00") +: Marker("\\\"\\\"\\\"\\\"") +: Close(")","\\\"\\\"") +: Close(")","\\\"") +: Comma +: Chunk("t") +: Close(")","\"") +: Close("}") +: Nil

    assert(expected === tokens)
  }

  test("grouping") {
    val tokens = Open("{") +: Open("(","\"") +: Chunk("201") +: Comma +: Open("(","\\\"") +: Chunk("101") +: Comma +: Open("(","\\\"\\\"") +: Chunk("test1'") +: Marker("\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"") +: Chunk("2001-01-03 13:21:00") +: Marker("\\\"\\\"") +: Comma +: Open("[","\\\"\\\"") +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-01 14:30:00") +: Marker("\\\"\\\"\\\"\\\"") +:
      Comma +: Marker("\\\"\\\"\\\"\\\"") +: Chunk("2010-01-03 15:30:00") +: Marker("\\\"\\\"\\\"\\\"") +: Close(")","\\\"\\\"") +: Close(")","\\\"") +: Comma +: Chunk("t") +: Close(")","\"") +: Close("}") +: Nil
    val root = grouping(tokens)

    val expected =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Open("(","\""),
          Chunk("201"),
          Comma,
          GroupToken(List(
            Open("(","\\\""),
            Chunk("101"),
            Comma,
            GroupToken(List(
              Open("(","\\\"\\\""),
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
              Open("[","\\\"\\\""),
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
              Close(")","\\\"\\\"")
            )),
            Close(")","\\\"")
          )),
          Comma,
          Chunk("t"),
          Close(")","\"")
        )),
        Close("}")
      ))

    assert(expected === root)

    val root1 = grouping(Tokenizer.tokenize("""{"(\"(test1'\",,,)"}"""))
    val expected1 =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Open("(", marker = "\""),
          GroupToken(List(
            Open("(", marker = "\\\""),
            Chunk("test1'"),
            Marker("\\\"")
          )),
          Comma,
          Null,
          Comma,
          Null,
          Comma,
          Null,
          Close(")", marker = "\"")
        )),
        Close("}")
      ))
    assert(expected1 === root1)

    val root2 = grouping(Tokenizer.tokenize(("""{"(,102,,)"}""")))
    val expected2 =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Open("(", marker = "\""),
          Null,
          Comma,
          Chunk("102"),
          Comma,
          Null,
          Comma,
          Null,
          Close(")", marker = "\"")
        )),
        Close("}")
      ))
    assert(expected2 === root2)

    val root3 = grouping(Tokenizer.tokenize("""{"(,,,)"}"""))
    val expected3 =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Open("(", marker = "\""),
          Null,
          Comma,
          Null,
          Comma,
          Null,
          Comma,
          Null,
          Close(")", marker = "\"")
        )),
        Close("}")
      ))
    assert(expected3 === root3)
  }

  test("get string") {
    val input =
      GroupToken(List(
        Open("[","\\\"\\\""),
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
        Close(")","\\\"\\\"")
      ))
    val rangeStr = getString(input, 2)

    val expected = """["2010-01-01 14:30:00","2010-01-03 15:30:00")"""
    assert(expected === rangeStr)
  }

  test("get children") {
    val input =
      GroupToken(List(
        Open("[","\\\"\\\""),
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
        Close(")","\\\"\\\"")
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

    assert(expected === children)
  }

  test("create string") {
    val input =
      GroupToken(List(
        Open("{"),
        GroupToken(List(
          Open("(","\""),
          Chunk("201"),
          GroupToken(List(
            Open("(","\\\""),
            Chunk("101"),
            GroupToken(List(
              Open("(","\\\"\\\""),
              Chunk("test1'"),
              Marker("\\\"\\\"")
            )),
            GroupToken(List(
              Marker("\\\"\\\""),
              Chunk("2001-01-03 13:21:00"),
              Marker("\\\"\\\"")
            )),
            GroupToken(List(
              Open("[","\\\"\\\""),
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
              Close(")","\\\"\\\"")
            )),
            Close(")","\\\"")
          )),
          Chunk("t"),
          Close(")","\"")
        )),
        Close("}")
      ))
    val pgStr = createString(input)

    val expected = """{"(201,\"(101,\"\"(test1'\"\",\"\"2001-01-03 13:21:00\"\",\"\"[\\\\\"\"2010-01-01 14:30:00\\\\\"\",\\\\\"\"2010-01-03 15:30:00\\\\\"\")\"\")\",t)"}"""

    assert(pgStr === expected)

    ///
    val input1 =
      GroupToken(List(
        Open("(","\""),
        Chunk("201"),
        GroupToken(List(
          Open("(","\\\""),
          Chunk("101"),
          GroupToken(List(
            Open("(","\\\"\\\""),
            Chunk("test1'"),
            Marker("\\\"\\\"")
          )),
          GroupToken(List(
            Marker("\\\"\\\""),
            Chunk("2001-01-03 13:21:00"),
            Marker("\\\"\\\"")
          )),
          GroupToken(List(
            Open("[","\\\"\\\""),
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
            Close(")","\\\"\\\"")
          )),
          Close(")","\\\"")
        )),
        Chunk("t"),
        Close(")","\"")
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