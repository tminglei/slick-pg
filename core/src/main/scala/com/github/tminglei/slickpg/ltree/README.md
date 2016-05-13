Supported ltree Oper/Functions
------------------------------

| Slick Oper/Function | PG Oper/Function |        Description            |            Example              | Result |
| ------------------- | ---------------- | ----------------------------- | ------------------------------- | ------ |
| @&gt;               | @&gt;            | is left argument an ancestor of right (or equal)? | 'Top.Science' @&gt; 'Top.Science.Astronomy'  | t    |
| &lt;@               | &lt;@            | is left argument a descendant of right (or equal)? | 'Top.Science' &lt;@ 'Top.Science.Astronomy' | f	   |
| ~                   | ~                | does ltree match lquery?      | 'Top.Science.Astronomy' ~ '*.Astronomy.*' | f 		|
| @@                  | @                | does ltree match ltxtquery?   | 'Top.Science.Astronomy' @ 'Astro* & !pictures@' | f	|
| &#124;&#124;        | &#124;&#124;     | concatenate ltree paths       | 'Top.Science'::ltree &#124;&#124; 'Astronomy.Stars'::ltree | 'Top.Science.Astronomy.Stars' |
| &#124;&#124;        | &#124;&#124;     | convert text to ltree and concatenate | 'Top.Science'::ltree &#124;&#124; 'Astronomy' | 'Top.Science.Astronomy' |
| &#124;&#124;:       | &#124;&#124;:    | convert text to ltree and concatenate | 'Top' &#124;&#124; 'Astronomy.Stars'::ltree | 'Top.Astronomy.Stars' |
| subltree            | subltree         | subpath of ltree from position start to position .. | subltree('Top.Child1.Child2',1,2) | Child1 |
| subpath             | subpath          | subpath of ltree starting at position offset, length len | subpath('Top.Child1.Child2',0,2) | Top.Child1 |
| nlevel              | nlevel           | number of labels in path      | nlevel('Top.Child1.Child2')     | 3      |
| index               | index            | position of first occurrence of b in a; -1 if not found | index('0.1.2.3.5.4.5.6.8.5.6.8','5.6') | 6 |
| lca                 | lca              | lowest common ancestor, i.e., longest common prefix of paths | lca(array['1.2.2.3'::ltree,'1.2.3']) | 1.2 |
