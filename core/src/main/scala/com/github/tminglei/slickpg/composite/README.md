Supported Composite type Oper/Functions
---------------------------------------

_ps: only type mapper supported currently_

To use it, pls declare your type mappers like this:
```scala
  trait MyCompositeSupport extends utils.PgCommonJdbcTypes with array.PgArrayJavaTypes { driver: PostgresDriver =>

    trait CompositeImplicts {
      import utils.TypeConverters.Util._
      
      utils.TypeConverters.register(mkCompositeConvFromString[Composite1])
      utils.TypeConverters.register(mkCompositeConvToString[Composite1])
      utils.TypeConverters.register(mkCompositeConvFromString[Composite2])
      utils.TypeConverters.register(mkCompositeConvToString[Composite2])
      
      implicit val composite1TypeMapper = new GenericJdbcType[Composite1]("composite1",
        mkCompositeConvFromString[Composite1], mkCompositeConvToString[Composite1])
      implicit val composite2TypeMapper = new GenericJdbcType[Composite2]("composite2",
        mkCompositeConvFromString[Composite2], mkCompositeConvToString[Composite2])
      implicit val composite1ArrayTypeMapper = new ArrayListJavaType[Composite1]("composite1",
        mkArrayConvFromString[Composite1], mkArrayConvToString[Composite1])
      implicit val composite2ArrayTypeMapper = new ArrayListJavaType[Composite2]("composite2",
        mkArrayConvFromString[Composite2], mkArrayConvToString[Composite2])
    }
  }
  
  object MyPostgresDriver1 extends MyPostgresDriver with MyCompositeSupport {
    override val Implicit = new ImplicitsPlus with CompositeImplicts {}
    override val simple = new SimpleQLPlus with CompositeImplicts {}
  }
```

then let it available where you declare the table that uses it
```scala
  import MyPostgresDriver1.simple._

  ...
  
  class TestTable(tag: Tag) extends Table[TestBean](tag, "CompositeTest") {
    def id = column[Long]("id")
    def comps = column[List[Composite2]]("comps", O.DBType("composite2[]"))
    
    def * = (id,comps) <> (TestBean.tupled, TestBean.unapply)
  }
  
  ...
```