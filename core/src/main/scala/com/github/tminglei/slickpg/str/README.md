Supported Search Oper/Functions
-------------------------------

| Slick Oper/Function | PG Oper/Function |       Description                |                Example                 |   Result    |
| ------------------- | ---------------- | -------------------------------- | -------------------------------------- | ----------- |
| ilike               | ilike            | correspond to `like`, match case-insensitive | 'test' ilike 'Test'        | t           |
| convert             | convert          | Convert string to dest_encoding. The original encoding is specified by src_encoding | convert('text_in_utf8', 'UTF8', 'LATIN1') | `text_in_utf8` represented in Latin-1 encoding (ISO 8859-1) |
| convertFrom         | convert_from     | Convert string to the database encoding. The original encoding is specified by src_encoding | convert_from('text_in_utf8', 'UTF8') | `text_in_utf8` represented in the current database encoding |
| convertTo           | convert_to       | Convert string to dest_encoding  | convert_to('some text', 'UTF8')        | `some text` represented in the UTF8 encoding |
| encode              | encode           | Encode binary data into a textual representation. Supported formats are: base64, hex, escape  | encode(E'123\\000\\001', 'base64') | MTIzAAE= |
| decode              | decode           | Decode binary data from textual representation in string. Options for format are same as in encode  | decode('MTIzAAE=', 'base64') | \x3132330001 |
| ~                   | ~                | Matches regular expression, case sensitive | 'thomas' ~ '.\*thomas.\*'    | t            |
| ~\*                 | ~\*              | Matches regular expression, case insensitive | 'thomas' ~\* '.\*Thomas.\*' | t           |
| !~                  | !~               | Does not match regular expression, case sensitive | 'thomas' !~ '.\*Thomas.\*' | t       |
| !~\*                | !~\*             | Does not match regular expression, case insensitive | 'thomas' !~\* '.\*vadim.\*' | t    |
