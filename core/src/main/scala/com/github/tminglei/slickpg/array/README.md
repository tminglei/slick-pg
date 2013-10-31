Supported Array Oper/Functions
------------------------------

| Slick Oper/Function | PG Oper/Function |        Description            |            Example              | Result |
| ------------------- | ---------------- | ----------------------------- | ------------------------------- | ------ |
| any                 | any              | expr operator ANY (array expr)| 3 = any(ARRAY[1,3])             |   t    |
| all                 | all              | expr operator ALL (array expr)| 4 > all(ARRAY[1,3])             |   t    |
| @>                  | @>               | contains                      | ARRAY[1,4,3] @> ARRAY[3,1]      |   t    |
| <@                  | <@               | is contained by               | ARRAY[2,7] <@ ARRAY[1,7,4,2,6]  |   t    |
| @&                  | &&               | overlap                       | ARRAY[1,4,3] && ARRAY[2,1]      |   t    |
| ++                  | &#124;&#124;     | array-to-array concatenation  | ARRAY[1,2,3] &#124;&#124; ARRAY[4,5,6] | {1,2,3,4,5,6} |
| +                   | &#124;&#124;     | array-to-element concatenation| ARRAY[4,5,6] &#124;&#124; 7     | {4,5,6,7} |
| +:                  | &#124;&#124;     | element-to-array concatenation| 3 &#124;&#124; ARRAY[4,5,6]     | {3,4,5,6} |
| length              | array_length     | length of the array/dimension | array_length(array[1,2,3], 1)   |   3    |
| unnest              | unnest           | expand array to a set of rows | unnest(ARRAY[1,2])              | 1<br/> 2<br/> (2 rows) |
