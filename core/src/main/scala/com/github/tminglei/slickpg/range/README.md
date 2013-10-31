Supported Range Oper/Functions
------------------------------

| Slick Oper/Function | PG Oper/Function |       Description       |                Example                | Result |
| ------------------- | ---------------- | ----------------------- | ------------------------------------- | ------ |
| @>                  | @>               | contains range          | int4range(2,4) @> int4range(2,3)      |   t    |
| @>^                 | @>               | contains element        | int4range(2,4) @> 3                   |   t    |
| <@:                 | <@               | range is contained by   | int4range(2,4) <@ int4range(1,7)      |   t    |
| <@^:                | <@               | element is contained by | 42 <@ int4range(1,7)                  |   f    |
| @&                  | &&               | overlap                 | int8range(3,7) && int8range(4,12)     |   t    |
| <<                  | <<               | strictly left of        | int8range(1,10) << int8range(100,110) |   t    |
| >>                  | >>               | strictly right of       | int8range(50,60) >> int8range(20,30)  |   t    |
| &<                  | &<               | does not extend to the right of | int8range(1,20) &< int8range(18,20)|   t   |
| &>                  | &>               | does not extend to the left of  | int8range(7,20) &> int8range(5,10) |   t   |
| -&#124;-            | -&#124;-         | is adjacent to          | numrange(1.1,2.2) -&#124;- numrange(2.2,3.3)|  t   |
| +                   | +                | union                   | numrange(5,15) + numrange(10,20)      | [5,20) |
| *                   | *                | intersection            | int8range(5,15) * int8range(10,20)    | [10,15)|
| -                   | -                | difference              | int8range(5,15) - int8range(10,20)    | [5,10) |
