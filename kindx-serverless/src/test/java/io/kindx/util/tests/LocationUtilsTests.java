package io.kindx.util.tests;

import io.kindx.util.LocationUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class LocationUtilsTests {

    @Test
    public void testPackedCirclesFactor2() {
        List<Map<String, Double>> points = LocationUtils.getOverlappingPackedCirclePoints(
                59.437515, 24.746583, 50000, 2
        );
        Assert.assertNotNull(points);
        Assert.assertEquals(points.size(), 16);

        Assert.assertEquals(12500.0,
                points.get(0).get("min_rad"),
                0.1);

        double radiusSquared = 12500.0 * 12500.0;

        Assert.assertEquals(Math.sqrt(radiusSquared + radiusSquared),
                points.get(0).get("max_rad"),
                0.1);
    }

    @Test
    public void testPackedCirclesFactor0() {
        List<Map<String, Double>> points = LocationUtils.getOverlappingPackedCirclePoints(
                59.437515, 24.746583, 50000, 0
        );
        Assert.assertNotNull(points);
        Assert.assertEquals(points.size(), 1);

        Assert.assertEquals(50000.0,
                points.get(0).get("min_rad"),
                0.1);

        double radiusSquared = 50000.0 * 50000.0;

        Assert.assertEquals(Math.sqrt(radiusSquared + radiusSquared),
                points.get(0).get("max_rad"),
                0.1);
    }


    @Test
    public void testPackedCirclesFactor3() {
        List<Map<String, Double>> points = LocationUtils.getOverlappingPackedCirclePoints(
                59.437515, 24.746583, 50000, 3
        );
        Assert.assertNotNull(points);
        Assert.assertEquals(points.size(), 64);

        Assert.assertEquals(6250.0,
                points.get(0).get("min_rad"),
                0.1);

        double radiusSquared = 6250.0 * 6250.0;

        Assert.assertEquals(Math.sqrt(radiusSquared + radiusSquared),
                points.get(0).get("max_rad"),
                0.1);
    }
}
