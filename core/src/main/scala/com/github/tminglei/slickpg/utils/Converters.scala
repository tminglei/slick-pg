package com.github.tminglei.slickpg
package utils

import scala.reflect.runtime.{universe => ru, currentMirror => runtimeMirror}
import scala.reflect.ClassTag

object Converters {
  @scala.annotation.implicitNotFound(msg = "No converter available for ${FROM} to ${TO}")
  trait Converter[FROM, TO] extends (FROM => TO)

  object Converter {
    def apply[FROM, TO](convert: (FROM => TO)) =
      new Converter[FROM, TO] {
        def apply(v: FROM) = convert(v)
      }
  }

  ///
  private[utils] case class CacheKey(val from: ru.Type, val to: ru.Type) {
    override def equals(o: Any) = {
      if (o.isInstanceOf[CacheKey]) {
        val that = o.asInstanceOf[CacheKey]
        from =:= that.from && to =:= that.to
      } else false
    }
  }

  private var converterMap = Map[CacheKey, Converter[_, _]]()

  private[utils] def internalGet(from: ru.Type, to: ru.Type) = {
    val cacheKey = CacheKey(from, to)
    converterMap.get(cacheKey).orElse({
      if (to <:< from) {
        converterMap += (cacheKey -> Converter((v: Any) => v))
        converterMap.get(cacheKey)
      } else None
    })
  }

  def register[FROM,TO](convert: (FROM => TO))(implicit from: ru.TypeTag[FROM], to: ru.TypeTag[TO]) = {
    println(s"register converter for ${from.tpe} => ${to.tpe}")
    converterMap += (CacheKey(from.tpe, to.tpe) -> Converter(convert))
  }

  def converter[FROM,TO](implicit from: ru.TypeTag[FROM], to: ru.TypeTag[TO]): Converter[FROM,TO] = {
    internalGet(from.tpe, to.tpe).map(_.asInstanceOf[Converter[FROM,TO]])
      .getOrElse(throw new IllegalArgumentException(s"Converter NOT FOUND for ${from.tpe} => ${to.tpe}"))
  }

  ////////////////////////////////////////////////////////////////
  object Util {
    import PGObjectTokenizer.PGElements._

    //
    private val strType  = ru.typeOf[String]
    private val elemType = ru.typeOf[Element]

    private def convertToValue(e: Element, toType: ru.Type): Any = e match {
      case ValueE(v)  => Converters.internalGet(strType, toType).get.asInstanceOf[Converter[String,_]](v)
      case _: CompositeE  => Converters.internalGet(elemType, toType).get.asInstanceOf[Converter[Element,_]](e)
      case ArrayE(el) => {
        val eType = toType.asInstanceOf[ru.TypeRef].args(0)
        el.map(e => convertToValue(e, eType))
      }
    }

    private def convertToElement(v: Any, fromType: ru.Type): Element = {
      (v, fromType) match {
        case (Some(realVal), _) => {
          val realType = fromType.asInstanceOf[ru.TypeRef].args(0)
          convertToElement(realVal, realType)
        }
        case (vList: List[_], _) => {
          val elemType = fromType.asInstanceOf[ru.TypeRef].args(0)
          ArrayE(vList.map(convertToElement(_, elemType)))
        }
        case (_, _) => {
          Converters.internalGet(fromType, elemType)
            .map(_.asInstanceOf[Converter[Any, Element]](v))
            .getOrElse(if (v == null || v == None) null else ValueE(v.toString) )
        }
      }
    }

    ///
    def mkConvFromElement[T <: AnyRef](implicit ev: ru.TypeTag[T]): Converter[Element, T] = {

      new Converter[Element, T] {
        private val thisType = ru.typeOf[T]
        private val optType = ru.typeOf[Option[_]]

        private val constructor = thisType.declaration(ru.nme.CONSTRUCTOR).asMethod
        private val ctorArgInfo = constructor.paramss.head.map(_.typeSignature).map(tpe =>
          if (tpe.typeConstructor =:= optType.typeConstructor) {
            val realType = tpe.asInstanceOf[ru.TypeRef].args(0)
            (realType, true)
          } else (tpe, false)
        )

        //--
        def apply(elem: Element): T = {
          val classMirror = runtimeMirror.reflectClass(thisType.typeSymbol.asClass)
          val ctorMirror = classMirror.reflectConstructor(constructor)

          val args = elem.asInstanceOf[CompositeE].members.zip(ctorArgInfo).map {
            case (e, (tpe, isOption)) => {
              val tv = convertToValue(e, tpe)
              if (isOption) Option(tv) else tv
            }
          }
          ctorMirror.apply(args: _*).asInstanceOf[T]
        }
      }
    }

    def mkArrayConvFromElement[T](implicit ev: ru.TypeTag[T]): Converter[Element, List[T]] = {

      new Converter[Element, List[T]] {
        private val thisType = ru.typeOf[T]
        private val optType = ru.typeOf[Option[_]]

        private val (realType, isOption) =
          if (thisType.typeConstructor =:= optType.typeConstructor) {
            val realType = thisType.asInstanceOf[ru.TypeRef].args(0)
            (realType, true)
          } else (thisType, false)

        //--
        def apply(elem: Element): List[T] = {
          elem.asInstanceOf[ArrayE].elements.map { e =>
            val tv = convertToValue(e, realType)
            if (isOption) Option(tv) else tv
          } map (_.asInstanceOf[T])
        }
      }
    }

    ////
    def mkConvToElement[T](implicit ev: ru.TypeTag[T], ev1: ClassTag[T]): Converter[T, Element] = {

      new Converter[T, Element] {
        private val thisType = ru.typeOf[T]

        private val constructor = thisType.declaration(ru.nme.CONSTRUCTOR).asMethod
        private val ctorFieldInfo = constructor.paramss.head.map(t => {
          val field = thisType.declaration(t.name).asTerm
          val tpe  = t.typeSignature
          (field, tpe)
        })

        //--
        def apply(v: T): Element = {
          val instanceMirror = runtimeMirror.reflect(v)
          CompositeE(
            ctorFieldInfo.map {
              case (field, tpe) => {
                val fv = instanceMirror.reflectField(field).get
                convertToElement(fv, tpe)
              }
            }
          )
        }
      }
    }

    def mkArrayConvToElement[T](implicit ev: ru.TypeTag[T], ev1: ClassTag[T]): Converter[List[T], Element] = {

      new Converter[List[T], Element] {
        private val thisType = ru.typeOf[T]

        def apply(vList: List[T]): Element = {
          ArrayE(
            vList.map { v =>
              convertToElement(v, thisType)
            }
          )
        }
      }
    }
  }
}