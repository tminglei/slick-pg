Supported Composite type Oper/Functions
---------------------------------------

_ps: only type mapper supported currently_

To use it, pls declare your type mappers like this:
```scala
  import utils.TypeConverters.Util._

  implicit val composite1TypeMapper = new ArrayListJavaType[Composite1]("composite1",
    mkArrayConvFromString[Composite1], mkArrayConvToString[Composite1])
  implicit val composite2TypeMapper = new ArrayListJavaType[Composite2]("composite2",
    mkArrayConvFromString[Composite2], mkArrayConvToString[Composite2])
  ...
```
and let it available where you declare the table that uses it
