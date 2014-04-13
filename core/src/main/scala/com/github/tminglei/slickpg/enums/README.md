The examples assume an enum type created as:
```
CREATE TYPE rainbow AS ENUM ('red', 'orange', 'yellow', 'green', 'blue', 'purple');
```

Supported Enum Oper/Functions
------------------------------

| Slick Oper/Function | PG Oper/Function |        Description            |            Example              | Result |
| ------------------- | ---------------- | ----------------------------- | ------------------------------- | ------ |
| first               | enum_first       | first value of the enum type  | enum_first(null::rainbow)       | red    |
| last                | enum_last        | last value of the enum type   | enum_last(null::rainbow)        | purple |
| all                 | enum_range       | all values of the enum type in ordered array  | enum_range(null::rainbow)  | {red,orange,yellow,<br/>green,blue,purple} |
| range               | enum_range       | range between two given enum values, as an ordered array | enum_range(<br/>'orange'::rainbow, <br/>'green'::rainbow) | {orange,yellow,green} |
