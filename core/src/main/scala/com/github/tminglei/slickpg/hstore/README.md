Supported HStore Oper/Functions
-------------------------------

| Slick Oper/Function | PG Oper/Function |        Description            |            Example              | Result |
| ------------------- | ---------------- | ----------------------------- | ------------------------------- | ------ |
| +>                  | ->               | get value for key             | 'a=>x, b=>y'::hstore -> 'a'     |   x    |
| >>                  | cast(hstore->k as T) | get value of type T for key   | cast('a=>3,b=>y'::hstore ->'a' as int) |  3  |
| ??                  | exist            | does hstore contain key?      | exist('a=>1','a')               |   t    |
| ?&                  | defined          | does hstore contain non-NULL value for key? | defined('a=>NULL','a') |   f   |
| '?' conflict with jdbc '?' | ?&        | does hstore contain all the keys? | 'a=>1,b=>2'::hstore ?& ARRAY['a','b'] |  t  |
| '?' conflict with jdbc '?' | ?&#124;   | does hstore contain any the keys? | 'a=>1,b=>2'::hstore ?&#124; ARRAY['b','c']  |  t  |
| @>                  | @>               | does left operand contain right?  | 'a=>b, b=>1, c=>NULL'::hstore @> 'b=>1' |   t   |
| <@:                 | <@               | is left operand contained in right? | 'a=>c'::hstore <@ 'a=>b, b=>1, c=>NULL' |  f  |
| @+                  | &#124;&#124;     | concatenate hstores           | 'a=>b, c=>d'::hstore &#124;&#124; 'c=>x, d=>q'::hstore | "a"=>"b", "c"=>"x", "d"=>"q" |
| @-                  | -                | delete matching pairs from left operand | 'a=>1, b=>2, c=>3'::hstore - 'a=>4, b=>2'::hstore | "a"=>"1", "c"=>"3" |
| --                  | -                | delete keys from left operand | 'a=>1, b=>2, c=>3'::hstore - ARRAY['a','b'] | "c"=>"3" |
| -/                  | -                | delete key from left operand  | 'a=>1, b=>2, c=>3'::hstore - 'b' | "a"=>"1", "c"=>"3"  |
