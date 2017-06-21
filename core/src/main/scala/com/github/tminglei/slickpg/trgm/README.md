Supported Search Oper/Functions
-------------------------------

| Slick Oper/Function | PG Oper/Function |       Description                                                                 |
| ------------------- | ---------------- | --------------------------------------------------------------------------------- |
| %                   | %                | Returns true if its arguments have a similarity that is greater than the current similarity threshold set by pg_trgm.similarity_threshold  |
| &lt;                | &lt;%            | Returns true if its first argument has the similar word in the second argument and they have a similarity that is greater than the current word similarity threshold set by pg_trgm.word_similarity_threshold  |
| %&gt;               | %&gt;            | Commutator of the &lt;% operator                                                  |
| &lt;-&gt;           | &lt;-&gt;        | Returns the "distance" between the arguments, that is one minus the similarity() value  |
| &lt;&lt;-&gt;       | &lt;&lt;-&gt;    | Returns the "distance" between the arguments, that is one minus the word_similarity() value  |
| &lt;-&gt;&gt;       | &lt;-&gt;&gt;    | Commutator of the &lt;&lt;-&gt; operator                                          |
| similarity          | similarity       | Returns a number that indicates how similar the two strings are                   |
| wordSimilarity      | word_similarity  | Returns a number that indicates how similar the first string to the most similar word of the second string  |
