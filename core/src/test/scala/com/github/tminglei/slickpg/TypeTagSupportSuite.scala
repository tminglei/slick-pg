package com.github.tminglei.slickpg

import izumi.reflect.Tag
import izumi.reflect.macrortti.LightTypeTag
import org.scalatest.funsuite.AnyFunSuite

class TypeTagSupportSuite extends AnyFunSuite {
  case class Foo(x: Int)
  class Around {
    case class Inner(y: String)
  }
  type AroundInner = Around#Inner
  def classFromTypeTag(tag: LightTypeTag): Class[_] = {
    val mungedRef = tag.ref.repr.replace("::", "$")
    Class.forName(mungedRef)
  }
  test("class from type tag") {
    classFromTypeTag(Tag[String].tag) === classOf[String]
    classFromTypeTag(Tag[Foo].tag) === classOf[Foo]
    classFromTypeTag(Tag[Around#Inner].tag) === classOf[Around#Inner]
    classFromTypeTag(Tag[AroundInner].tag) === classOf[Around#Inner]
  }
}
