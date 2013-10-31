Supported Search Oper/Functions
-------------------------------

| Slick Oper/Function | PG Oper/Function |       Description                |                Example                 |   Result    |
| ------------------- | ---------------- | -------------------------------- | -------------------------------------- | ----------- |
| tsQuery             | to_tsquery       | normalize words and convert to tsquery | to_tsquery('english', 'The & Fat & Rats') | 'fat' & 'rat' |
| tsVector            | to_tsvector      | reduce document text to tsvector | to_tsvector('english', 'The Fat Rats') | 'fat':2 'rat':3 |
| @@                  | @@               | tsvector matches tsquery ?       | to_tsvector('fat cats ate rats') @@ to_tsquery('cat & rat') | t |
| @+                  | &#124;&#124;     | concatenate tsvectors            | 'a:1 b:2'::tsvector &#124;&#124; 'c:1 d:2 b:3'::tsvector | 'a':1 'b':2,5 'c':3 'd':4 |
| @&                  | &&               | AND tsquerys together            | 'fat &#124; rat'::tsquery && 'cat'::tsquery | ( 'fat' &#124; 'rat' ) & 'cat' |
| @&#124;             | &#124;&#124;     | OR tsquerys together             | 'fat &#124; rat'::tsquery &#124;&#124; 'cat'::tsquery | ( 'fat' &#124; 'rat' ) &#124; 'cat' |
| !!                  | !!               | negate a tsquery                 | !! 'cat'::tsquery                      | !'cat'      |
| @>                  | @>               | tsquery contains another ?       | 'cat'::tsquery @> 'cat & rat'::tsquery |     f       |
| tsHeadline          | ts_headline      | display a query match            | ts_headline('x y z', 'z'::tsquery)     | x y <b>z</b>|
| tsRank              | ts_rank          | rank document for query          | ts_rank(textsearch, query)             | 0.818       |
| tsRankCD            | ts_rank_cd       | rank document for query using cover density | ts_rank_cd('{0.1, 0.2, 0.4, 1.0}', textsearch, query) | 2.01317  |
