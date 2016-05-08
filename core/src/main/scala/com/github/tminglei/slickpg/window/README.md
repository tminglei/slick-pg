Supported Aggregate Functions
------------------------------

| Slick Function  | PG Function      |        Description            |
| --------------- | ---------------- | ----------------------------- |
| rowNumber       | row_number       | number of the current row within its partition, counting from 1 |
| rank            | rank             | rank of the current row with gaps; same as row_number of its first peer |
| denseRank       | dense_rank       | rank of the current row without gaps; this function counts peer groups |
| percentRank     | percent_rank     | relative rank of the current row: (rank - 1) / (total rows - 1) |
| cumeDist        | cume_dist        | relative rank of the current row: (number of rows preceding or peer with current row) / (total rows) |
| ntile           | ntile            | integer ranging from 1 to the argument value, dividing the partition as equally as possible |
| lag             | lag              | returns value evaluated at the row that is offset rows before the current row within the partition |
| lead            | lead             | returns value evaluated at the row that is offset rows after the current row within the partition |
| firstValue      | first_value      | returns value evaluated at the row that is the first row of the window frame |
| lastValue       | last_value       | returns value evaluated at the row that is the last row of the window frame |
| nthValue        | nth_value        | returns value evaluated at the row that is the nth row of the window frame |
