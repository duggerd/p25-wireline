function calcDistanceByGPoints(points)
{
   // http://www.linuxpromagazine.com/issue/64/Google_Maps_API.pdf
   var distance = 0.0;
   var p1 = points[0];
   var rad = Math.PI / 180.0;
   for (var i = 1; i <points.length; i++)
   {
      var p2 = points[i];
      var lat1 = p1.point.y * rad;
      var lon1 = p1.point.x * rad;
      var lat2 = p2.point.y * rad;
      var lon2 = p2.point.x * rad;
      distance += 6378.7 * Math.acos(Math.sin(lat1) * Math.sin(lat2) +
             Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1));
      p1 = p2;
   }
   return distance;
}

/*** need to check accuracy
function calculateDistance2( lat1, lon1, lat2, lon2)
{
   var R = 6378.7;
   var rad = Math.PI / 180.0;
   //var R = 6371;
   //var dLat = (lat2-lat1).toRad();
   //var dLon = (lon2-lon1).toRad();
   var dLat = (lat2-lat1) * rad;
   var dLon = (lon2-lon1) * rad;
   var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
           Math.cos(lat1) * Math.cos(lat2) *
           Math.sin(dLon/2) * Math.sin(dLon/2);
   var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
   var d = R * c;
   return d;
}
 ***/
//==================================================================
function calculateDistance( lat_1, lon_1, lat_2, lon_2)
{
   var R = 6378.7;
   var rad = Math.PI / 180.0;
   var lat1 = lat_1 * rad;
   var lon1 = lon_1 * rad;
   var lat2 = lat_2 * rad;
   var lon2 = lon_2 * rad;

   var d = R * Math.acos(Math.sin(lat1) * Math.sin(lat2) +
           Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2-lon1));

   // convert from km to miles
   return d * 0.621371192;
}
/*** different formula
function calculateDistance( lat1, lon1, lat2, lon2)
{
   var R = 6371.0;
   var rad = Math.PI / 180.0;

   var dLat = (lat2-lat1) * rad;
   var dLon = (lon2-lon1) * rad;
   var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
           Math.cos(lat1*rad) * Math.cos(lat2*rad) *
           Math.sin(dLon/2) * Math.sin(dLon/2);
   var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
   var d = R * c;
   // convert from km to miles
   return d * 0.621371192;
}
 ***/
