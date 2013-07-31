Supported data type's operators/functions
-----------------------------------------

#### Array
| Slick Operator/Function | PG Operator/Function |        Description            |            Example              | Result |
| ----------------------- | -------------------- | ----------------------------- | ------------------------------- | ------ |
| any                     | any                  | expr operator ANY (array expr)| 3 = any(ARRAY[1,3])             |   t    |
| all                     | all                  | expr operator ALL (array expr)| 4 > all(ARRAY[1,3])             |   t    |
| @>                      | @>                   | contains                      | ARRAY[1,4,3] @> ARRAY[3,1]      |   t    |
| <@                      | <@                   | is contained by               | ARRAY[2,7] <@ ARRAY[1,7,4,2,6]  |   t    |
| @&                      | &&                   | overlap                       | ARRAY[1,4,3] && ARRAY[2,1]      |   t    |
| ++                      | &#124;&#124;         | array-to-array concatenation  | ARRAY[1,2,3] &#124;&#124; ARRAY[4,5,6] | {1,2,3,4,5,6} |
| +                       | &#124;&#124;         | array-to-element concatenation| ARRAY[4,5,6] &#124;&#124; 7     | {4,5,6,7} |
| +:                      | &#124;&#124;         | element-to-array concatenation| 3 &#124;&#124; ARRAY[4,5,6]     | {3,4,5,6} |
| length                  | array_length         | length of the array/dimension | array_length(array[1,2,3], 1)   |   3    |
| unnest                  | unnest               | expand array to a set of rows | unnest(ARRAY[1,2])              | 1<br/>2<br/>(2 rows) |


### Datetime
| Slick Operator/Function | PG Operator/Function |       Description       |                Example                         |           Result         |
| ----------------------- | -------------------- | ----------------------- | ---------------------------------------------- | ------------------------ |
| +++                     | +                    | timestamp + interval    | timestamp '2001-09-28 01:00' + interval '23 hours'|timestamp '2001-09-29 00:00:00'|
| -                       | -                    | timestamp - timestamp   | timestamp '2001-09-29 03:00' - timestamp '2001-09-27 12:00'|interval '1 day 15:00:00'|
| --                      | -                    | timestamp - time        | timestamp '2001-09-29 03:00' - time '03:00' | timestamp '2001-09-29 00:00'|
| ---                     | -                    | timestame - interval    | timestamp '2001-09-28 23:00' - interval '23 hours'|timestamp '2001-09-28 00:00:00'|
| age                     | age                  | age(timestamp[, timestamp])| age(timestamp '2001-04-10', timestamp '1957-06-13')|43 years 9 mons 27 days|
| age                     | age                  | age(timestamp)          | age(timestamp '1957-06-13')                    | 43 years 9 mons 27 days  |
| part                    | date_part/extract    | date_part(text, timestamp) | date_part('hour', timestamp '2001-02-16 20:38:40') | 20                |
| trunc                   | date_trunc           | date_trunc(text, timestamp)| date_trunc('hour', timestamp '2001-02-16 20:38:40') | 2001-02-16 20:00:00 |
| +                       | +                    | date + time             | date '2001-09-28' + time '03:00'       | timestamp '2001-09-28 03:00:00'  |
| ++                      | +                    | date + int              | date '2001-10-01' - integer '7'        | date '2001-09-24'                |
| +++                     | +                    | date + interval         | date '2001-09-28' + interval '1 hour'  | timestamp '2001-09-28 01:00:00'  |
| -                       | -                    | date - date             | date '2001-10-01' - date '2001-09-28'  | integer '3' (days)               |
| --                      | -                    | date - int              | date '2001-10-01' - integer '7'        | date '2001-09-24'                |
| ---                     | -                    | date - interval         | date '2001-09-28' - interval '1 hour'  | timestamp '2001-09-27 23:00:00'  |
| +                       | +                    | time + date             | time '03:00' + date '2001-09-28'       | timestamp '2001-09-28 03:00:00'  |
| +++                     | +                    | time + interval         | time '05:00' - interval '2 hours'      | time '03:00:00'                  |
| -                       | -                    | time - time             | time '05:00' - time '03:00'            | interval '02:00:00'              |
| ---                     | -                    | time - interval         | time '05:00' - interval '2 hours'      | time '03:00:00'                  |
| +                       | +                    | interval + interval     | interval '1 day' + interval '1 hour'   | interval '1 day 01:00:00'        |
| unary_-                 | -                    | - inteval               | - interval '23 hours'                  | interval '-23:00:00'             |
| -                       | -                    | interval - interval     | interval '1 day' - interval '1 hour'   | interval '1 day -01:00:00'       |
| *                       | *                    | interval * factor       | double precision '3.5' * interval '1 hour'| interval '03:30:00'           |
| /                       | /                    | interval / factor       | interval '1 hour' / double precision '1.5'| interval '00:40:00'           |
             

#### Range
| Slick Operator/Function | PG Operator/Function |       Description       |                Example                | Result |
| ----------------------- | -------------------- | ----------------------- | ------------------------------------- | ------ |
| @>                      | @>                   | contains range          | int4range(2,4) @> int4range(2,3)      |   t    |
| @>^                     | @>                   | contains element        | int4range(2,4) @> 3                   |   t    |
| <@:                     | <@                   | range is contained by   | int4range(2,4) <@ int4range(1,7)      |   t    |
| <@^:                    | <@                   | element is contained by | 42 <@ int4range(1,7)                  |   f    |
| @&                      | &&                   | overlap                 | int8range(3,7) && int8range(4,12)     |   t    |
| <<                      | <<                   | strictly left of        | int8range(1,10) << int8range(100,110) |   t    |
| >>                      | >>                   | strictly right of       | int8range(50,60) >> int8range(20,30)  |   t    |
| &<                      | &<                   | does not extend to the right of | int8range(1,20) &< int8range(18,20)|   t   |
| &>                      | &>                   | does not extend to the left of  | int8range(7,20) &> int8range(5,10) |   t   |
| -&#124;-                | -&#124;-             | is adjacent to          | numrange(1.1,2.2) -&#124;- numrange(2.2,3.3)|  t   |
| +                       | +                    | union                   | numrange(5,15) + numrange(10,20)      | [5,20) |
| *                       | *                    | intersection            | int8range(5,15) * int8range(10,20)    | [10,15)|
| -                       | -                    | difference              | int8range(5,15) - int8range(10,20)    | [5,10) |

#### HStore
| Slick Operator/Function | PG Operator/Function |        Description            |            Example              | Result |
| ----------------------- | -------------------- | ----------------------------- | ------------------------------- | ------ |
| +>                      | ->                   | get value for key             | 'a=>x, b=>y'::hstore -> 'a'     |   x    |
| >>                      | cast(hstore->k as T) | get value of type T for key   | cast('a=>3,b=>y'::hstore ->'a' as int) |  3  |
| ??                      | exist                | does hstore contain key?      | exist('a=>1','a')               |   t    |
| ?&                      | defined              | does hstore contain non-NULL value for key? | defined('a=>NULL','a') |   f   |
| can't support because of '?' | ?&              | does hstore contain all the keys? | 'a=>1,b=>2'::hstore ?& ARRAY['a','b'] |  t  |
| can't support because of '?' | ?&#124;         | does hstore contain any the keys? | 'a=>1,b=>2'::hstore ?&#124; ARRAY['b','c']  |  t  |
| @>                      | @>                   | does left operand contain right?  | 'a=>b, b=>1, c=>NULL'::hstore @> 'b=>1' |   t   |
| <@:                     | <@                   | is left operand contained in right? | 'a=>c'::hstore <@ 'a=>b, b=>1, c=>NULL' |  f  |
| @+                      | &#124;&#124;         | concatenate hstores           | 'a=>b, c=>d'::hstore &#124;&#124; 'c=>x, d=>q'::hstore | "a"=>"b", "c"=>"x", "d"=>"q" |
| @-                      | -                    | delete key from left operand  | 'a=>1, b=>2, c=>3'::hstore - 'b' | "a"=>"1", "c"=>"3"  |
| --                      | -                    | delete keys from left operand | 'a=>1, b=>2, c=>3'::hstore - ARRAY['a','b'] | "c"=>"3" |
| -/                      | -                    | delete matching pairs from left operand | 'a=>1, b=>2, c=>3'::hstore - 'a=>4, b=>2'::hstore | "a"=>"1", "c"=>"3" |

#### Search
| Slick Operator/Function | PG Operator/Function |       Description                |                Example                 |   Result    |
| ----------------------- | -------------------- | -------------------------------- | -------------------------------------- | ----------- |
| tsQuery                 | to_tsquery           | normalize words and convert to tsquery | to_tsquery('english', 'The & Fat & Rats') | 'fat' & 'rat' |
| tsVector                | to_tsvector          | reduce document text to tsvector | to_tsvector('english', 'The Fat Rats') | 'fat':2 'rat':3 |
| @@                      | @@                   | tsvector matches tsquery ?       | to_tsvector('fat cats ate rats') @@ to_tsquery('cat & rat') | t |
| @+                      | &#124;&#124;         | concatenate tsvectors            | 'a:1 b:2'::tsvector &#124;&#124; 'c:1 d:2 b:3'::tsvector | 'a':1 'b':2,5 'c':3 'd':4 |
| @&                      | &&                   | AND tsquerys together            | 'fat &#124; rat'::tsquery && 'cat'::tsquery | ( 'fat' &#124; 'rat' ) & 'cat' |
| @&#124;                 | &#124;&#124;         | OR tsquerys together             | 'fat &#124; rat'::tsquery &#124;&#124; 'cat'::tsquery | ( 'fat' &#124; 'rat' ) &#124; 'cat' |
| !!                      | !!                   | negate a tsquery                 | !! 'cat'::tsquery                      | !'cat'      |
| @>                      | @>                   | tsquery contains another ?       | 'cat'::tsquery @> 'cat & rat'::tsquery |     f       |
| tsHeadline              | ts_headline          | display a query match            | ts_headline('x y z', 'z'::tsquery)     | x y <b>z</b>|
| tsRank                  | ts_rank              | rank document for query          | ts_rank(textsearch, query)             | 0.818       |
| tsRankCD                | ts_rank_cd           | rank document for query using cover density | ts_rank_cd('{0.1, 0.2, 0.4, 1.0}', textsearch, query) | 2.01317  |

#### Geometry
| Slick Operator/Function | PostGIS Operator/Function |           Description                                  |            Example            |
| ----------------------- | ------------------------- | ------------------------------------------------------ | ----------------------------- |
| @&&                     | &&                        | if A's 2D bounding box intersects B's 2D bounding box  | geomA && geomB                |
| @&&&                    | &&&                       | if A's 3D bounding box intersects B's 3D bounding box  | geomA &&& geomB               |
| @>                      | ~                         | A's bounding box contains B's                          | geomA ~ geomB                 |
| <@                      | @                         | if A's bounding box is contained by B's                | geomA @ geomB                 |
| <->                     | <->                       | the distance between two points                        | geomA <-> geomB               |
| <#>                     | <#>                       | the distance between bounding box of 2 geometries      | geomA <#> geomB               |
| &<                      | &<                        | if A's bounding box overlaps or is to the left of B's  | geomA &< geomB                |
| <<                      | <<                        | if A's bounding box is strictly to the left of B's     | geomA << geomB                |
| &<&#124;                | &<&#124;                  | if A's bounding box overlaps or is below B's           | geomA &<&#124; geomB          |
| <<&#124;                | <<&#124;                  | if A's bounding box is strictly below B's              | goemA <<&#124; geomB          |
| &>                      | &>                        | if A' bounding box overlaps or is to the right of B's  | geomA &> geomB                |
| >>                      | >>                        | if A's bounding box is strictly to the right of B's    | geomA >> geomB                |
| &#124;&>                | &#124;&>                  | if A's bounding box overlaps or is above B's           | geomA &#124;&> geomB          |
| &#124;>>                | &#124;>>                  | if A's bounding box is strictly above B's              | geomA &#124;>> geomB          |
| geomFromText            | ST_GeomFromText           | create a ST_Geometry from a Well-Known Text            | ST_GeomFromText(wkt)          |
| geomFromWKB             | ST_GeomFromWKB            | create a geometry from a Well-Known Binary             | ST_GeomFromWKB(wkb)           |
| geomFromEWKT            | ST_GeomFromEWKT           | create a ST_Geometry from a Extended Well-Known Text   | ST_GeomFromEWKT(ewkt)         |
| geomFromEWKB            | ST_GeomFromWKB            | create a geometry from a Well-Known Binary             | ST_GeomFromWKB(ewkb)          |
| geomFromGML             | ST_GeomFromGML            | create a geometry from input GML                       | ST_GeomFromGML(gml[, srid])   |
| geomFromKML             | ST_GeomFromKML            | create a geometry from input KML                       | ST_GeomFromKML(kml)           |
| geomFromGeoJSON         | ST_GeomFromGeoJSON        | create a geometry from input geojson                   | ST_GeomFromGeoJSON( json)     |
| makeBox                 | ST_MakeBox2D              | Creates a BOX2D defined by the given point geometries  | ST_MakeBox2D( pointLowLeft, pointUpRight)  |
| makePoint               | ST_MakePoint<br/>ST_MakePointM | Creates a 2D,3DZ or 4D point geometry             | ST_MakePoint(x,y)<br/>ST_MakePointM(x,y,m) |
| geomType                | ST_GeometryType           | the geometry type of the ST_Geometry value             | ST_GeometryType(geom)         |
| srid                    | ST_SRID                   | the spatial reference identifier for the ST_Geometry   | ST_SRID(geom)                 |
| isValid                 | ST_IsValid                | if the ST_Geometry is well formed                      | ST_IsValid(geom[, flags])     |
| isClosed                | ST_IsClosed               | if the LINESTRING's start and end points are coincident| ST_IsClosed(geom)             |
| isCollection            | ST_IsCollection           | if the argument is a collection (MULTI*, GEOMETRYCOLLECTION, ...) | ST_IsCollection(geom) |
| isEmpty                 | ST_IsEmpty                | if this Geometry is an empty geometrycollection, polygon, point etc | ST_IsEmpty(geom) |
| isRing                  | ST_IsRing                 | if this LINESTRING is both closed and simple           | ST_IsRing(geom)               |
| isSimple                | ST_IsSimple               | if this Geometry has no anomalous geometric points, such as self intersection or self tangency | ST_IsSimple(geom) |
| hasArc                  | ST_HasArc                 | if a geometry or geometry collection contains a circular string | ST_HasArc(geom)      |
| area                    | ST_Area                   | area of the surface if it's a polygon or multi-polygon | ST_Area(geom)            |
| boundary                | ST_Boundary               | closure of the combinatorial boundary of the Geometry  | ST_Boundary(geom)             |
| dimension               | ST_Dimension              | inherent dimension of this Geometry object, which must be less than or equal to the coordinate dimension | ST_Dimension(geom) |
| coordDim                | ST_CoordDim               | the coordinate dimension of the ST_Geometry value      | ST_CoordDim(geom)             |
| nDims                   | ST_NDims                  | coordinate dimension of the geometry                   | ST_NDims(geom)                |
| nPoints                 | ST_NPoints                | number of points (vertexes) in a geometry              | ST_NPoints(geom)              |
| nRings                  | ST_NRings                 | number of rings if the geometry is a polygon or multi-polygon | ST_NRings(geom)        |
| asBinary                | ST_AsBinary               | Well-Known Binary of the geometry without SRID         | ST_AsBinary(geom[, NDRorXDR]  |
| asText                  | ST_AsText                 | Well-Known Text of the geometry without SRID           | ST_AsText(geom)               |
| asLatLonText            | ST_AsLatLonText           | Degrees, Minutes, Seconds representation of the point  | ST_AsLatLonText(geom[, format]) |
| asEWKB                  | ST_AsEWKB                 | Well-Known Binary of the geometry with SRID            | ST_AsEWKB(geom[, NDRorXDR])   |
| asEWKT                  | ST_AsEWKT                 | Well-Known Text of the geometry with SRID              | ST_AsEWKT(geom)               |
| asHEXEWKB               | ST_AsHEXEWKB              | HEXEWKB format text of the geometry with SRID          | ST_AsHEXEWKB(geom[, NDRorXDR])|
| asGeoJSON               | ST_AsGeoJSON              | GeoJSON format text of the geometry                    | ST_AsGeoJSON( [ver, ]geom, maxdigits, options) |
| asGeoHash               | ST_GeoHash                | GeoHash representation (geohash.org) of the geometry   | ST_GeoHash(geom, maxchars)    |
| asGML                   | ST_AsGML                  | GML format text of the geometry                        | ST_AsGML([ver, ]geom, maxdigits, options)   |
| asKML                   | ST_AsKML                  | KML format text of the geometry                        | ST_AsKML([ver, ]geom, maxdigits[, nprefix]) |
| asSVG                   | ST_AsSVG                  | SVG format text of the geometry                        | ST_AsSVG(geom, rel, maxdigits)     |
| asX3D                   | ST_AsX3D                  | X3D format text of the geometry                        | ST_AsX3D(geom, maxdigits, options) |
| gEquals                 | ST_Equals                 | if the given geometries represent the same geometry    | ST_Equals(geomA, geomB)       |
| orderingEquals          | ST_OrderingEquals         | if the given geometries represent the same geometry and points are in the same directional order | ST_OrderingEquals( geomA, geomB)   |
| overlaps                | ST_Overlaps               | if the Geometries share space, are of the same dimension, but are not completely contained by each other | ST_Overlaps(geomA, geomB) |
| intersects              | ST_Intersects             | if the geometries "spatially intersect in 2D"          | ST_Intersects(geomA, geomB)   |
| crosses                 | ST_Crosses                | if the supplied geometries have some, but not all, interior points in common | ST_Crosses(geomA, geomB) |
| disjoint                | ST_Disjoint               | if the Geometries do not "spatially intersect"         | ST_Disjoint(geomA, geomB)     |
| contains                | ST_Contains               | if geometry A contains geometry B                      | ST_Contains(geomA, geomB)     |
| containsProperly        | ST_ContainsProperly       | if geometry A contains geometry B and no boundary      | ST_ContainsProperly(geomA, geomB) |
| within                  | ST_Within                 | if the geometry A is completely inside geometry B      | ST_Within(geomA, geomB)       |
| dWithin                 | ST_DWithin                | if the geometry are within the specified distance of another | ST_DWithin(geomA, geomB, distance) |
| dFullyWithin            | ST_DFullyWithin           | if all of the geometries are within the specified distance of one another | ST_DFullyWithin( geomA, geomB, distance) |
| touches                 | ST_Touches                | if the geometries have at least one point in common, but their interiors do not intersect | ST_Touches(geomA, geomB)|
| relate                  | ST_Relate                 | if this geometry is spatially related to another       | ST_Relate(geomA, geomB, intersectionMatrixPattern)   |
| relatePattern           | ST_Relate                 | maximum intersectionMatrixPattern that relates the 2 geometries | ST_Relate(geomA, geomB[, boundaryNodeRule]) |
| azimuth                 | ST_Azimuth                | angle in radians from the horizontal of the vector defined by pointA and pointB | ST_Azimuth(pointA, pointB)  |
| centroid                | ST_Centroid               | geometric center of the geometry                       | ST_Centroid(geom)             |
| closestPoint            | ST_ClosestPoint           | the first point of the shortest line from geomA to geomB | ST_ClosestPoint( geomA, geomB) |
| pointOnSurface          | ST_PointOnSurface         | a POINT guaranteed to lie on the surface               | ST_PointOnSurface( geom)       |
| project                 | ST_Project                | a POINT projected from a start point using a distance in meters and bearing (azimuth) in radians | ST_Project(geog, distance, azimuth) |
| length                  | ST_Length                 | 2d length of the geometry if it is a linestring or multilinestring | ST_Length(geom)   |
| length3d                | ST_3DLength               | 3d length of the geometry if it is a linestring or multi-linestring| ST_3DLength(geom) |
| perimeter               | ST_Perimeter              | length measurement of the boundary of an ST_Surface or ST_MultiSurface geometry | ST_Perimeter(geom) |
| distance                | ST_Distance               | minimum distance between the two geometries            | ST_Distance(geomA, geomB      |
| distanceSphere          | ST_Distance_Sphere        | minimum distance in meters between two lon/lat geometries | ST_Distance_Sphere( geomA, geomB) |
| maxDistance             | ST_MaxDistance            | largest distance between the two geometries            | ST_MaxDistance(geomA, geomB)  |
| hausdorffDistance       | ST_HausdorffDistance      | Hausdorff distance between two geometries, a measure of how similar or dissimilar they are.  | ST_HausdorffDistance( geomA, geomB[, densifyFrac]) |
| longestLine             | ST_LongestLine            | longest line points of the two geometries              | ST_LongestLine(geomA, geomB)  |
| shortestLine            | ST_ShortestLine           | shortest line between the two geometries               | ST_ShortestLine(geomA, geomB) |
| setSRID                 | ST_SetSRID                | set the SRID on a geometry                             | ST_SetSRID(geom, srid)        |
| transform               | ST_Transform              | new geometry with its coordinates transformed to the SRID | ST_Transform(geom, srid)   |
| simplify                | ST_Simplify               | "simplified" version of the given geometry using the Douglas-Peucker algorithm   | ST_Simplify(geom, tolerance) |
| simplifyPreserveTopology| ST_SimplifyPreserveTopology | "simplified" version of the given geometry using the Douglas-Peucker algorithm | ST_SimplifyPreserveTopology( geom, tolerance) |
| removeRepeatedPoints    | ST_RemoveRepeatedPoints   | version of the given geometry with duplicated points removed | ST_RemoveRepeatedPoints( geom) |
| difference              | ST_Difference             | part of geometry A that does not intersect with geometry B   | ST_Difference(geomA, geomB)    |
| symDifference           | ST_SymDifference          | portions of A and B that do not intersect              | ST_SymDifference(geomA, geomB)|
| intersection            | ST_Intersection           | the shared portion of geomA and geomB                  | ST_Intersection(geomA, geomB) |
| sharedPaths             | ST_SharedPaths            | collection of shared paths by the two input linestrings/multilinestrings | ST_SharedPaths(line1, line2) |
| split                   | ST_Split                  | collection of geometries resulting by splitting a geometry | ST_Split(geomA, bladeGeomB) |
| minBoundingCircle       | ST_MinimumBoundingCircle  | smallest circle polygon that can fully contain a geometry  | ST_MinimumBoundingCircle( geom, num_segs_per_qt_circ)|
| buffer                  | ST_Buffer                 | a geometry that all its points is less than or equal to distance from the geometry | ST_Buffer(geom, radius[, bufferStyles]) |
| multi                   | ST_Multi                  | a geometry as a MULTI* geometry                        | ST_Multi(geom)                |
| lineMerge               | ST_LineMerge              | lineString(s) formed by sewing together a MULTILINESTRING | ST_LineMerge(geom)   |
| collectionExtract       | ST_CollectionExtract      | a (multi)geometry consisting only of elements of the specified type from the geometry  | ST_CollectionExtract( geom, type)    |
| collectionHomogenize    | ST_CollectionHomogenize   | "simplest" representation of the geometry collection   | ST_CollectionHomogenize( geom)|
| addPoint                | ST_AddPoint               | Adds a point to a LineString before point [position]   | ST_AddPoint(lineGeom, point, position) |
| setPoint                | ST_SetPoint               | Replace point N of linestring with given point         | ST_SetPoint(lineGeom, position, point) |
| removePoint             | ST_RemovePoint            | Removes point from a linestring                        | ST_RemovePoint(lineGeom, offset) |
| reverse                 | ST_Reverse                | the geometry with vertex order reversed                | ST_Reverse(geom)              |
| scale                   | ST_Scale                  | Scales the geometry to a new size by multiplying the ordinates with the parameters | ST_Scale(geom, xfactor, yfactor[, zfactor])  |
| segmentize              | ST_Segmentize             | geometry having no segment longer than the given distance from the geometry | ST_Segmentize( geom, maxLength) |
| snap                    | ST_Snap                   | Snap segments and vertices of input geometry to vertices of a reference geometry   | ST_Snap(geom, refGeom, tolerance) |
| translate               | ST_Translate              | Translates the geometry to a new location using the numeric parameters as offsets  | ST_Translate(geom, deltax, deltay[, deltaz]) |
