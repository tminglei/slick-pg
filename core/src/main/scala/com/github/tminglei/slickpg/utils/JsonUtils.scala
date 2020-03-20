package com.github.tminglei.slickpg.utils

import java.util.regex.Pattern

object JsonUtils {

  private val CLEAN_PATTERNS = List(
    (Pattern.compile("\u0000", Pattern.LITERAL), ""),
    (Pattern.compile("(\\\\\\\\)"), "_/_$1__"), // replace to protect `\\u0000`
    (Pattern.compile("\\u0000", Pattern.LITERAL), ""),
    (Pattern.compile("_/_(\\\\\\\\)__"), "$1") // replace back `\\u0000`
  )
  /** remove ctrl char `\u0000` which will fail json parsers, but keep normal like `\\u0000` */
  def clean(str: String): String = {
    var ret = str
    for (p <- CLEAN_PATTERNS) {
      ret = p._1.matcher(ret).replaceAll(p._2)
    }
    ret
  }
}
