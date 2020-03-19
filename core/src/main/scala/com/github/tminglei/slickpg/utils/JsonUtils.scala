package com.github.tminglei.slickpg.utils

object JsonUtils {
  // remove ctrl char `\u0000` which will fail json parsers, but keep normal like `\\u0000`
  def clean(str: String): String = {
    str.replace("\u0000", "")
      .replaceAll("(\\\\\\\\)", "_/_$1__") // replace to protect `\\u0000`
      .replace("\\u0000", "")
      .replaceAll("_/_(\\\\\\\\)__", "$1") // replace back `\\u0000`
  }
}
