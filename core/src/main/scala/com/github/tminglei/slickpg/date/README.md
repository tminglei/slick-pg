Supported Date/Time Oper/Functions
---------------------------------

| Slick Oper/Function | PG Oper/Function |       Description       |                Example                         |           Result         |
| ------------------- | ---------------- | ----------------------- | ---------------------------------------------- | ------------------------ |
| +++                 | +                | timestamp + interval    | timestamp '2001-09-28 01:00' + interval '23 hours'|timestamp '2001-09-29 00:00:00'|
| -                   | -                | timestamp - timestamp   | timestamp '2001-09-29 03:00' - timestamp '2001-09-27 12:00'|interval '1 day 15:00:00'|
| --                  | -                | timestamp - time        | timestamp '2001-09-29 03:00' - time '03:00' | timestamp '2001-09-29 00:00'|
| ---                 | -                | timestame - interval    | timestamp '2001-09-28 23:00' - interval '23 hours'|timestamp '2001-09-28 00:00:00'|
| age                 | age              | age(timestamp[, timestamp])| age(timestamp '2001-04-10', timestamp '1957-06-13')|43 years 9 mons 27 days|
| age                 | age              | age(timestamp)          | age(timestamp '1957-06-13')                    | 43 years 9 mons 27 days  |
| part                | date_part/extract| date_part(text, timestamp) | date_part('hour', timestamp '2001-02-16 20:38:40') | 20                |
| trunc               | date_trunc       | date_trunc(text, timestamp)| date_trunc('hour', timestamp '2001-02-16 20:38:40') | 2001-02-16 20:00:00 |
| +                   | +                | date + time             | date '2001-09-28' + time '03:00'       | timestamp '2001-09-28 03:00:00'  |
| ++                  | +                | date + int              | date '2001-10-01' - integer '7'        | date '2001-09-24'                |
| +++                 | +                | date + interval         | date '2001-09-28' + interval '1 hour'  | timestamp '2001-09-28 01:00:00'  |
| -                   | -                | date - date             | date '2001-10-01' - date '2001-09-28'  | integer '3' (days)               |
| --                  | -                | date - int              | date '2001-10-01' - integer '7'        | date '2001-09-24'                |
| ---                 | -                | date - interval         | date '2001-09-28' - interval '1 hour'  | timestamp '2001-09-27 23:00:00'  |
| +                   | +                | time + date             | time '03:00' + date '2001-09-28'       | timestamp '2001-09-28 03:00:00'  |
| +++                 | +                | time + interval         | time '05:00' - interval '2 hours'      | time '03:00:00'                  |
| -                   | -                | time - time             | time '05:00' - time '03:00'            | interval '02:00:00'              |
| ---                 | -                | time - interval         | time '05:00' - interval '2 hours'      | time '03:00:00'                  |
| +                   | +                | interval + interval     | interval '1 day' + interval '1 hour'   | interval '1 day 01:00:00'        |
| unary_-             | -                | - inteval               | - interval '23 hours'                  | interval '-23:00:00'             |
| -                   | -                | interval - interval     | interval '1 day' - interval '1 hour'   | interval '1 day -01:00:00'       |
| *                   | *                | interval * factor       | double precision '3.5' * interval '1 hour'| interval '03:30:00'           |
| /                   | /                | interval / factor       | interval '1 hour' / double precision '1.5'| interval '00:40:00'           |
