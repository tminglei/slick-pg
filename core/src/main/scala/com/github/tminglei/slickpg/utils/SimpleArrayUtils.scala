package com.github.tminglei.slickpg.utils

object SimpleArrayUtils {
  import PgTokenHelper._
  
  def fromString[T](convert: String => T)(arrString: String): Option[List[T]] =
    grouping(Tokenizer.tokenize(arrString)) match {
      case Null => None
      case root => Some(getChildren(root).map {
        case Null  => null.asInstanceOf[T]
        case token => convert(getString(token, 0))
      })
    }

  def mkString[T](ToString: T => String)(value: List[T]): String =
    createString (value match {
      case null  => Null
      case vList => {
        val members = Open("{") +: vList.map {
            case v => if (v == null) Null else Chunk(ToString(v))
          } :+ Close("}")
        GroupToken(members)
      }
    })
}
