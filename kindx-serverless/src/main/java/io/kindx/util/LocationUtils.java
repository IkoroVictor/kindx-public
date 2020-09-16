package io.kindx.util;


import org.elasticsearch.common.geo.GeoUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationUtils {

    public static final String LAT_KEY = "lat";
    public static final String LON_KEY = "lon";
    public static final String MIN_RADIUS_KEY = "min_rad";
    public static final String MAX_RADIUS_KEY = "max_rad";

    public static List<Map<String, Double>> getOverlappingPackedCirclePoints(double lat, double lon,
                                                                             double radius, int reductionFactor) {

        //Implements circle packing by making a square out of the radius, breaking them into
        // equal 4 ^ reductionFactor smaller squares and returning their respective min and max radius (of the circle in each square).
        // The 'min' radius is half the length of the square. The max radius is half the diagonal length of the square
        List<Map<String, Double>> points = new ArrayList<>();
        packedCirclePoints(lat, lon, radius, reductionFactor, points);
        return points;
    }


    private static void packedCirclePoints(double lat, double lon,
                                           double radius, int reductionFactor,
                                           List<Map<String, Double>> points) {
        //reduced square radius with pythagoras
        double radiusSquared = (radius * radius);
        double maxRadius = Math.sqrt(radiusSquared + radiusSquared);
        if (reductionFactor == 0) {
            Map<String, Double> map =  new HashMap<>();
            map.put(LAT_KEY, lat);
            map.put(LON_KEY, lon);
            map.put(MIN_RADIUS_KEY, radius);
            map.put(MAX_RADIUS_KEY, maxRadius);
            points.add(map);
            return;
        }

        //reduced square radius with pythagoras
        double reducedRadius = maxRadius/ 2.0;
        double halvedRadius = radius / 2.0;

        //Top left
        double[] reducedPoint =  destinationCoordinates(lat, lon, 360 - 45, reducedRadius);
        packedCirclePoints(reducedPoint[0], reducedPoint[1], halvedRadius, reductionFactor - 1, points);

        //Top right
        reducedPoint =  destinationCoordinates(lat, lon, 45, reducedRadius);
        packedCirclePoints(reducedPoint[0], reducedPoint[1], halvedRadius, reductionFactor - 1, points);

        //bottom left
        reducedPoint =  destinationCoordinates(lat, lon, 180 + 45, reducedRadius);
        packedCirclePoints(reducedPoint[0], reducedPoint[1], halvedRadius, reductionFactor - 1, points);

        //bottom right
        reducedPoint =  destinationCoordinates(lat, lon, 90 + 45, reducedRadius);
        packedCirclePoints(reducedPoint[0], reducedPoint[1], halvedRadius, reductionFactor - 1, points);

    }


    private static double[] destinationCoordinates(double lat, double lon,
                                                   double bearing,
                                                   double distance) {
        if (bearing > 360 || bearing < 0) {
            throw new IllegalArgumentException("Invalid bearing  " + bearing);
        }

        double angularDistance = distance / GeoUtils.EARTH_MEAN_RADIUS;

        double radianLat = Math.toRadians(lat);
        double radianLon = Math.toRadians(lon);
        double radianBearing = Math.toRadians(bearing);

        double newLat =  Math.asin(
                (Math.sin(radianLat) * Math.cos(angularDistance))
                        +  (Math.cos(radianLat) * Math.sin(angularDistance) * Math.cos(radianBearing))
        );

        double newLon  =  radianLon + Math.atan2(
                Math.sin(radianBearing) * Math.sin(angularDistance) * Math.cos(radianLat),
                Math.cos(angularDistance) - Math.sin(radianLat) * Math.sin(newLat)
        );

        return new double[] {Math.toDegrees(newLat), Math.toDegrees(newLon)};

    }
}
