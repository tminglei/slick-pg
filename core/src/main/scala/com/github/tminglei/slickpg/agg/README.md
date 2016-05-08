Supported Aggregate Functions
------------------------------

#### General-Purpose Aggregate Functions
| Slick Function  | PG Function      |        Description            |
| --------------- | ---------------- | ----------------------------- |
| arrayAgg        | array_agg        | input values, including nulls, concatenated into an array |
| stringAgg       | string_agg       | input values concatenated into a string, separated by delimiter |
| avg             | avg              | the average (arithmetic mean) of all input values |
| bitAnd          | bit_and          | the bitwise AND of all non-null input values, or null if none |
| bitOr           | bit_or           | the bitwise OR of all non-null input values, or null if none |
| boolAnd         | bool_and         | true if all input values are true, otherwise false |
| boolOr          | bool_or          | true if at least one input value is true, otherwise false |
| count           | count            | number of input rows |
| every           | every            | equivalent to bool_and |
| max             | max              | maximum value of expression across all input values |
| min             | min              | minimum value of expression across all input values |
| sum             | sum              | sum of expression across all input values  |

#### Aggregate Functions for Statistics
| Slick Function  | PG Function      |        Description            |
| --------------- | ---------------- | ----------------------------- |
| corr            | corr             | correlation coefficient       |
| covarPop        | covar_pop        | population covariance         |
| covarSamp       | covar_samp       | sample covariance             |
| regrAvgX        | regr_avgx        | average of the independent variable (sum(X)/N) |
| regrAvgY        | regr_avgy        | average of the dependent variable (sum(Y)/N) |
| regrCount       | regr_count       | number of input rows in which both expressions are nonnull |
| regrIntercept   | regr_intercept   | y-intercept of the least-squares-fit linear equation determined by the (X, Y) pairs |
| regrR2          | regr_r2          | square of the correlation coefficient |
| regrSlope       | regr_slope       | slope of the least-squares-fit linear equation determined by the (X, Y) pairs |
| regrSxx         | regr_sxx         | sum(X^2) - sum(X)^2/N ("sum of squares" of the independent variable) |
| regrSxy         | regr_sxy         | sum(X*Y) - sum(X) * sum(Y)/N ("sum of products" of independent times dependent variable) |
| regrSyy         | regr_syy         | sum(Y^2) - sum(Y)^2/N ("sum of squares" of the dependent variable) |
| stdDev          | stddev           | historical alias for stddev_samp |
| stdDevPop       | stddev_pop       | population standard deviation of the input values |
| stdDevSamp      | stddev_samp      | sample standard deviation of the input values |
| variance        | variance         | historical alias for var_samp |
| varPop          | var_pop          | population variance of the input values (square of the population standard deviation) |
| varSamp         | var_samp         | sample variance of the input values (square of the sample standard deviation) |

#### Ordered-Set Aggregate Functions
| Slick Function  | PG Function      |        Description            |
| --------------- | ---------------- | ----------------------------- |
| mode            | mode             | returns the most frequent input value |
| percentileCont  | percentile_cont  | continuous percentile: returns a value corresponding to the specified fraction in the ordering |
| percentileDisc  | percentile_disc  | discrete percentile: returns the first input value whose position in the ordering equals or exceeds the specified fraction |

#### Hypothetical-Set Aggregate Functions
| Slick Function  | PG Function      |        Description            |
| --------------- | ---------------- | ----------------------------- |
| rank            | rank             | rank of the hypothetical row, with gaps for duplicate rows |
| denseRank       | dense_rank       | rank of the hypothetical row, without gaps |
| percentRank     | percent_rank     | relative rank of the hypothetical row, ranging from 0 to 1 |
| cumeDist        | cume_dist        | relative rank of the hypothetical row, ranging from 1/N to 1 |
