Supported Geometry Oper/Functions
---------------------------------

#### Geometry Constructors
| Slick Oper/Function | PostGIS Oper/Function |           Description                                  |            Example            |
| ------------------- | --------------------- | ------------------------------------------------------ | ----------------------------- |
| geomFromText        | ST_GeomFromText       | create a ST_Geometry from a Well-Known Text            | ST_GeomFromText(wkt)          |
| geomFromWKB         | ST_GeomFromWKB        | create a geometry from a Well-Known Binary             | ST_GeomFromWKB(wkb)           |
| geomFromEWKT        | ST_GeomFromEWKT       | create a ST_Geometry from a Extended Well-Known Text   | ST_GeomFromEWKT(ewkt)         |
| geomFromEWKB        | ST_GeomFromWKB        | create a geometry from a Well-Known Binary             | ST_GeomFromWKB(ewkb)          |
| geomFromGML         | ST_GeomFromGML        | create a geometry from input GML                       | ST_GeomFromGML(gml[, srid])   |
| geomFromKML         | ST_GeomFromKML        | create a geometry from input KML                       | ST_GeomFromKML(kml)           |
| geomFromGeoJSON     | ST_GeomFromGeoJSON    | create a geometry from input geojson                   | ST_GeomFromGeoJSON( json)     |
| makeBox             | ST_MakeBox2D          | Creates a BOX2D defined by the given point geometries  | ST_MakeBox2D( pointLowLeft, pointUpRight)  |
| makeBox3d           | ST_3DMakeBox          | Creates a BOX3D defined by the given 3d point geometries | ST_3DMakeBox( point3dLowLeft, point3dUpRight)  |
| makeEnvelope        | ST_MakeEnvelope       | Creates a rectangular Polygon formed from the given minimums and maximums  | ST_MakeEnvelope(xmin, ymin, xmax, ymax, srid=unknown) | 
| makePoint           | ST_MakePoint<br/>ST_MakePointM | Creates a 2D,3DZ or 4D point geometry             | ST_MakePoint(x,y)<br/>ST_MakePointM(x,y,m) |
| makeLine            | ST_MakeLine           | Creates a Linestring from point or line geometries     | ST_MakeLine(point1, point2)   |
| makePolygon         | ST_MakePolygon        | Creates a Polygon formed by the given CLOSED linestring | ST_MakePolygon(linestring)   |

#### Geometry Operators
| Slick Oper/Function | PostGIS Oper/Function |           Description                                  |            Example            |
| ------------------- | --------------------- | ------------------------------------------------------ | ----------------------------- |
| @&&                 | &&                    | if A's 2D bounding box intersects B's 2D bounding box  | geomA && geomB                |
| @&&&                | &&&                   | if A's 3D bounding box intersects B's 3D bounding box  | geomA &&& geomB               |
| @>                  | ~                     | A's bounding box contains B's                          | geomA ~ geomB                 |
| <@                  | @                     | if A's bounding box is contained by B's                | geomA @ geomB                 |
| <->                 | <->                   | the distance between two points                        | geomA <-> geomB               |
| <#>                 | <#>                   | the distance between bounding box of 2 geometries      | geomA <#> geomB               |
| &<                  | &<                    | if A's bounding box overlaps or is to the left of B's  | geomA &< geomB                |
| <<                  | <<                    | if A's bounding box is strictly to the left of B's     | geomA << geomB                |
| &<&#124;            | &<&#124;              | if A's bounding box overlaps or is below B's           | geomA &<&#124; geomB          |
| <<&#124;            | <<&#124;              | if A's bounding box is strictly below B's              | goemA <<&#124; geomB          |
| &>                  | &>                    | if A' bounding box overlaps or is to the right of B's  | geomA &> geomB                |
| &gt;>               | &gt;>                 | if A's bounding box is strictly to the right of B's    | geomA >> geomB                |
| &#124;&>            | &#124;&>              | if A's bounding box overlaps or is above B's           | geomA &#124;&> geomB          |
| &#124;>>            | &#124;>>              | if A's bounding box is strictly above B's              | geomA &#124;>> geomB          |

#### Geometry Accessors
| Slick Oper/Function | PostGIS Oper/Function |           Description                                  |            Example            |
| ------------------- | --------------------- | ------------------------------------------------------ | ----------------------------- |
| geomType            | ST_GeometryType       | the geometry type of the ST_Geometry value             | ST_GeometryType(geom)         |
| srid                | ST_SRID               | the spatial reference identifier for the ST_Geometry   | ST_SRID(geom)                 |
| isValid             | ST_IsValid            | if the ST_Geometry is well formed                      | ST_IsValid(geom[, flags])     |
| isClosed            | ST_IsClosed           | if the LINESTRING's start and end points are coincident| ST_IsClosed(geom)             |
| isCollection        | ST_IsCollection       | if the argument is a collection (MULTI*, GEOMETRYCOLLECTION, ...) | ST_IsCollection(geom) |
| isEmpty             | ST_IsEmpty            | if this Geometry is an empty geometrycollection, polygon, point etc | ST_IsEmpty(geom) |
| isRing              | ST_IsRing             | if this LINESTRING is both closed and simple           | ST_IsRing(geom)               |
| isSimple            | ST_IsSimple           | if this Geometry has no anomalous geometric points, such as self intersection or self tangency | ST_IsSimple(geom) |
| hasArc              | ST_HasArc             | if a geometry or geometry collection contains a circular string | ST_HasArc(geom)      |
| area                | ST_Area               | area of the surface if it's a polygon or multi-polygon | ST_Area(geom)            |
| boundary            | ST_Boundary           | closure of the combinatorial boundary of the Geometry  | ST_Boundary(geom)             |
| dimension           | ST_Dimension          | inherent dimension of this Geometry object, which must be less than or equal to the coordinate dimension | ST_Dimension(geom) |
| coordDim            | ST_CoordDim           | the coordinate dimension of the ST_Geometry value      | ST_CoordDim(geom)             |
| nDims               | ST_NDims              | coordinate dimension of the geometry                   | ST_NDims(geom)                |
| nPoints             | ST_NPoints            | number of points (vertexes) in a geometry              | ST_NPoints(geom)              |
| nRings              | ST_NRings             | number of rings if the geometry is a polygon or multi-polygon | ST_NRings(geom)        |
| x                   | ST_X                  | Return the X coordinate of the point                   | ST_X(point)                   |
| y                   | ST_Y                  | Return the Y coordinate of the point                   | ST_Y(point)                   |
| z                   | ST_Z                  | Return the Z coordinate of the point                   | ST_Z(point)                   |
| xmin                | ST_XMin               | Returns X minima of a bounding box 2d or 3d or a geometry | ST_XMin(Box3D(geom))       |
| xmax                | ST_XMax               | Returns X maxima of a bounding box 2d or 3d or a geometry | ST_XMax(Box3D(geom))       |
| ymin                | ST_YMin               | Returns Y minima of a bounding box 2d or 3d or a geometry | ST_YMin(Box3D(geom))       |
| ymax                | ST_YMax               | Returns Y maxima of a bounding box 2d or 3d or a geometry | ST_YMax(Box3D(geom))       |
| zmin                | ST_ZMin               | Returns Z minima of a bounding box 2d or 3d or a geometry | ST_ZMin(Box3D(geom))       |
| zmax                | ST_ZMax               | Returns Z maxima of a bounding box 2d or 3d or a geometry | ST_ZMax(Box3D(geom))       |
| zmflag              | ST_Zmflag             | Returns ZM (dimension semantic) flag of the geometries as a small int. Values are: 0=2d, 1=3dm, 2=3dz, 3=4d | ST_Zmflag(geom) |

#### Geometry Outputs
| Slick Oper/Function | PostGIS Oper/Function |           Description                                  |            Example            |
| ------------------- | --------------------- | ------------------------------------------------------ | ----------------------------- |
| asBinary            | ST_AsBinary           | Well-Known Binary of the geometry without SRID         | ST_AsBinary(geom[, NDRorXDR]  |
| asText              | ST_AsText             | Well-Known Text of the geometry without SRID           | ST_AsText(geom)               |
| asLatLonText        | ST_AsLatLonText       | Degrees, Minutes, Seconds representation of the point  | ST_AsLatLonText(geom[, format]) |
| asEWKB              | ST_AsEWKB             | Well-Known Binary of the geometry with SRID            | ST_AsEWKB(geom[, NDRorXDR])   |
| asEWKT              | ST_AsEWKT             | Well-Known Text of the geometry with SRID              | ST_AsEWKT(geom)               |
| asHEXEWKB           | ST_AsHEXEWKB          | HEXEWKB format text of the geometry with SRID          | ST_AsHEXEWKB(geom[, NDRorXDR])|
| asGeoJSON           | ST_AsGeoJSON          | GeoJSON format text of the geometry                    | ST_AsGeoJSON( [ver, ]geom, maxdigits, options) |
| asGeoHash           | ST_GeoHash            | GeoHash representation (geohash.org) of the geometry   | ST_GeoHash(geom, maxchars)    |
| asGML               | ST_AsGML              | GML format text of the geometry                        | ST_AsGML([ver, ]geom, maxdigits, options)   |
| asKML               | ST_AsKML              | KML format text of the geometry                        | ST_AsKML([ver, ]geom, maxdigits[, nprefix]) |
| asSVG               | ST_AsSVG              | SVG format text of the geometry                        | ST_AsSVG(geom, rel, maxdigits)     |
| asX3D               | ST_AsX3D              | X3D format text of the geometry                        | ST_AsX3D(geom, maxdigits, options) |

#### Spatial Relationships
| Slick Oper/Function | PostGIS Oper/Function |           Description                                  |            Example            |
| ------------------- | --------------------- | ------------------------------------------------------ | ----------------------------- |
| gEquals             | ST_Equals             | if the given geometries represent the same geometry    | ST_Equals(geomA, geomB)       |
| orderingEquals      | ST_OrderingEquals     | if the given geometries represent the same geometry and points are in the same directional order | ST_OrderingEquals( geomA, geomB)   |
| overlaps            | ST_Overlaps           | if the Geometries share space, are of the same dimension, but are not completely contained by each other | ST_Overlaps(geomA, geomB) |
| intersects          | ST_Intersects         | if the geometries "spatially intersect in 2D"          | ST_Intersects(geomA, geomB)   |
| crosses             | ST_Crosses            | if the supplied geometries have some, but not all, interior points in common | ST_Crosses(geomA, geomB) |
| disjoint            | ST_Disjoint           | if the Geometries do not "spatially intersect"         | ST_Disjoint(geomA, geomB)     |
| contains            | ST_Contains           | if geometry A contains geometry B                      | ST_Contains(geomA, geomB)     |
| containsProperly    | ST_ContainsProperly   | if geometry A contains geometry B and no boundary      | ST_ContainsProperly(geomA, geomB) |
| within              | ST_Within             | if the geometry A is completely inside geometry B      | ST_Within(geomA, geomB)       |
| dWithin             | ST_DWithin            | if the geometry are within the specified distance of another | ST_DWithin(geomA, geomB, distance) |
| dFullyWithin        | ST_DFullyWithin       | if all of the geometries are within the specified distance of one another | ST_DFullyWithin( geomA, geomB, distance) |
| touches             | ST_Touches            | if the geometries have at least one point in common, but their interiors do not intersect | ST_Touches(geomA, geomB)|
| relate              | ST_Relate             | if this geometry is spatially related to another       | ST_Relate(geomA, geomB, intersectionMatrixPattern)   |
| relatePattern       | ST_Relate             | maximum intersectionMatrixPattern that relates the 2 geometries | ST_Relate(geomA, geomB[, boundaryNodeRule]) |

#### Spatial Measurements
| Slick Oper/Function | PostGIS Oper/Function |           Description                                  |            Example            |
| ------------------- | --------------------- | ------------------------------------------------------ | ----------------------------- |
| azimuth             | ST_Azimuth            | angle in radians from the horizontal of the vector defined by pointA and pointB | ST_Azimuth(pointA, pointB)  |
| centroid            | ST_Centroid           | geometric center of the geometry                       | ST_Centroid(geom)             |
| closestPoint        | ST_ClosestPoint       | the first point of the shortest line from geomA to geomB | ST_ClosestPoint( geomA, geomB) |
| pointOnSurface      | ST_PointOnSurface     | a POINT guaranteed to lie on the surface               | ST_PointOnSurface( geom)       |
| project             | ST_Project            | a POINT projected from a start point using a distance in meters and bearing (azimuth) in radians | ST_Project(geog, distance, azimuth) |
| length              | ST_Length             | 2d length of the geometry if it is a linestring or multilinestring | ST_Length(geom)   |
| length3d            | ST_3DLength           | 3d length of the geometry if it is a linestring or multi-linestring| ST_3DLength(geom) |
| perimeter           | ST_Perimeter          | length measurement of the boundary of an ST_Surface or ST_MultiSurface geometry | ST_Perimeter(geom) |
| distance            | ST_Distance           | minimum distance between the two geometries            | ST_Distance(geomA, geomB      |
| distanceSphere      | ST_Distance_Sphere    | minimum distance in meters between two lon/lat geometries | ST_Distance_Sphere( geomA, geomB) |
| maxDistance         | ST_MaxDistance        | largest distance between the two geometries            | ST_MaxDistance(geomA, geomB)  |
| hausdorffDistance   | ST_HausdorffDistance  | Hausdorff distance between two geometries, a measure of how similar or dissimilar they are.  | ST_HausdorffDistance( geomA, geomB[, densifyFrac]) |
| longestLine         | ST_LongestLine        | longest line points of the two geometries              | ST_LongestLine(geomA, geomB)  |
| shortestLine        | ST_ShortestLine       | shortest line between the two geometries               | ST_ShortestLine(geomA, geomB) |

#### Geometry Processing
| Slick Oper/Function | PostGIS Oper/Function |           Description                                  |            Example            |
| ------------------- | --------------------- | ------------------------------------------------------ | ----------------------------- |
| setSRID             | ST_SetSRID            | set the SRID on a geometry                             | ST_SetSRID(geom, srid)        |
| transform           | ST_Transform          | new geometry with its coordinates transformed to the SRID | ST_Transform(geom, srid)   |
| simplify            | ST_Simplify           | "simplified" version of the given geometry using the Douglas-Peucker algorithm   | ST_Simplify(geom, tolerance) |
| simplifyPreserveTopology| ST_SimplifyPreserveTopology | "simplified" version of the given geometry using the Douglas-Peucker algorithm | ST_SimplifyPreserveTopology( geom, tolerance) |
| removeRepeatedPoints| ST_RemoveRepeatedPoints | version of the given geometry with duplicated points removed | ST_RemoveRepeatedPoints( geom) |
| difference          | ST_Difference         | part of geometry A that does not intersect with geometry B   | ST_Difference(geomA, geomB)    |
| symDifference       | ST_SymDifference      | portions of A and B that do not intersect              | ST_SymDifference(geomA, geomB)|
| intersection        | ST_Intersection       | the shared portion of geomA and geomB                  | ST_Intersection(geomA, geomB) |
| sharedPaths         | ST_SharedPaths        | collection of shared paths by the two input linestrings/multilinestrings | ST_SharedPaths(line1, line2) |
| split               | ST_Split              | collection of geometries resulting by splitting a geometry | ST_Split(geomA, bladeGeomB) |
| minBoundingCircle   | ST_MinimumBoundingCircle| smallest circle polygon that can fully contain a geometry  | ST_MinimumBoundingCircle( geom, num_segs_per_qt_circ)|
| buffer              | ST_Buffer             | a geometry that all its points is less than or equal to distance from the geometry | ST_Buffer(geom, radius[, bufferStyles]) |
| multi               | ST_Multi              | a geometry as a MULTI* geometry                        | ST_Multi(geom)                |
| lineMerge           | ST_LineMerge          | lineString(s) formed by sewing together a MULTILINESTRING | ST_LineMerge(geom)   |
| collectionExtract   | ST_CollectionExtract  | a (multi)geometry consisting only of elements of the specified type from the geometry  | ST_CollectionExtract( geom, type)    |
| collectionHomogenize| ST_CollectionHomogenize | "simplest" representation of the geometry collection   | ST_CollectionHomogenize( geom)|
| addPoint            | ST_AddPoint           | Adds a point to a LineString before point [position]   | ST_AddPoint(lineGeom, point, position) |
| setPoint            | ST_SetPoint           | Replace point N of linestring with given point         | ST_SetPoint(lineGeom, position, point) |
| removePoint         | ST_RemovePoint        | Removes point from a linestring                        | ST_RemovePoint(lineGeom, offset) |
| reverse             | ST_Reverse            | the geometry with vertex order reversed                | ST_Reverse(geom)              |
| scale               | ST_Scale              | Scales the geometry to a new size by multiplying the ordinates with the parameters | ST_Scale(geom, xfactor, yfactor[, zfactor])  |
| segmentize          | ST_Segmentize         | geometry having no segment longer than the given distance from the geometry | ST_Segmentize( geom, maxLength) |
| snap                | ST_Snap               | Snap segments and vertices of input geometry to vertices of a reference geometry   | ST_Snap(geom, refGeom, tolerance) |
| translate           | ST_Translate          | Translates the geometry to a new location using the numeric parameters as offsets  | ST_Translate(geom, deltax, deltay[, deltaz]) |
