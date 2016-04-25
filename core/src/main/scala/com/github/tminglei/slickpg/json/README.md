Supported JSON Oper/Functions
-----------------------------

| Slick Oper/Function | PG Oper/Function |        Description            |            Example              | Result |
| ------------------- | ---------------- | ----------------------------- | ------------------------------- | ------ |
| ~>                  | ->               | Get JSON array element        | '[1,2,3]'::json->2              | 3      |
| ~>>					  | ->>					 | Get JSON array element as text| '[1,2,3]'::json->>2 			   | "3"	|
| +>					  | ->					 | Get JSON object field 		 | '{"a":1,"b":2}'::json->'b'	   | 2 		|
| +>>					  | ->> 				 | Get JSON object field as text | '{"a":1,"b":2}'::json->>'b'	   | "2"	|
| #>					  | #>					 | Get JSON object at specified path | '{"a":[1,2,3],"b":[4,5,6]}'::json#>'{a,2}' | 3 |
| #>>					  | #>>					 | Get JSON object at specified path as text | '{"a":[1,2,3],"b":[4,5,6]}'::json#>>'{a,2}' | "3" |
| &#124;&#124;  | &#124;&#124; | Concatenate two jsonb values into a new jsonb value | '["a", "b"]'::jsonb || '["c", "d"]'::jsonb | ["a", "b", "c", "d"] |
| -             | -            | Delete key/value pair or **string** element from left operand | '{"a": "b"}'::jsonb - 'a' | {} |
| #-            | #-           | Delete the field or element with specified path | '["a", {"b":1}]'::jsonb #- '{1,b}' | ["a"] |
| arrayLength 		  | json_array_length	 | Returns elem number of outermost JSON array | json_array_length('[1,2,3,{"f1":1,"f2":[5,6]},4]') | 5 |
| arrayElements 	  | json_array_elements| Expands JSON array to set of JSON elements | json_array_elements('[1,true, [2,false]]') | value<br/> -------------<br/> 1<br/> true<br/> [2,false] |
| objectKeys 			  | json_object_keys 	 | Returns set of keys in outermost JSON object | json_object_keys('{"f1":"abc","f2":{"f3":"a", "f4":"b"}}') | json_object_keys<br/> ----------------<br/> f1<br/> f2 |
| jsonb_set     | jsonb_set     | Returns target with the section designated by path replaced by **new_value** | jsonb_set('[{"f1":1,"f2":null},2]', '{0,f3}','[2,3,4]') | [{"f1": 1, "f2": null, "f3": [2, 3, 4]}, 2] |

