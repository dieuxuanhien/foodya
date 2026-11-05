package com.foodya.backend.domain.value_objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Projects a GPS point onto an encoded route polyline (point-to-segment projection).
 *
 * <p>Raw GPS jumps 5–30 m in urban areas; snapping to the route gives the client a smooth marker that
 * follows the road rather than teleporting across buildings.
 */
public final class RouteSnapper {

    private static final double EARTH_RADIUS_M = 6_371_000.0;

    private RouteSnapper() {}

    /**
     * @param encodedPolyline Google encoded polyline of the planned route
     * @param routeDurationSeconds total duration of the planned route
     * @param lat raw GPS latitude
     * @param lng raw GPS longitude
     * @return the snapped position plus progress/ETA, or {@code null} when there is no usable route
     */
    public static SnappedPosition snap(String encodedPolyline, int routeDurationSeconds, double lat, double lng) {
        if (encodedPolyline == null || encodedPolyline.isBlank()) return null;
        List<LatLng> points = PolylineCodec.decode(encodedPolyline);
        if (points.size() < 2) return null;

        // Local equirectangular projection around the GPS point. Accurate to well under a metre at
        // the segment lengths involved here, and avoids repeated trig in the inner loop.
        double latScale = Math.toRadians(1.0) * EARTH_RADIUS_M;
        double lngScale = latScale * Math.cos(Math.toRadians(lat));

        double px = lng * lngScale;
        double py = lat * latScale;

        double bestDistSq = Double.MAX_VALUE;
        int bestSeg = 0;
        double bestT = 0;

        for (int i = 0; i < points.size() - 1; i++) {
            LatLng a = points.get(i);
            LatLng b = points.get(i + 1);
            double ax = a.lng() * lngScale, ay = a.lat() * latScale;
            double bx = b.lng() * lngScale, by = b.lat() * latScale;
            double abx = bx - ax, aby = by - ay;
            double lenSq = abx * abx + aby * aby;
            double t = lenSq == 0 ? 0 : ((px - ax) * abx + (py - ay) * aby) / lenSq;
            t = Math.max(0, Math.min(1, t));
            double cx = ax + t * abx;
            double cy = ay + t * aby;
            double dx = px - cx, dy = py - cy;
            double distSq = dx * dx + dy * dy;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestSeg = i;
                bestT = t;
            }
        }

        return interpolate(points, bestSeg, bestT, latScale, lngScale, routeDurationSeconds);
    }

    private static SnappedPosition interpolate(List<LatLng> points, int seg, double t,
                                               double latScale, double lngScale, int durationSeconds) {
        List<Double> cumulative = cumulativeMeters(points, latScale, lngScale);
        double total = cumulative.get(cumulative.size() - 1);
        if (total <= 0) return null;

        double covered = cumulative.get(seg) + t * (cumulative.get(seg + 1) - cumulative.get(seg));
        double progress = Math.max(0.0, Math.min(1.0, covered / total));
        double remaining = total - covered;

        // Interpolate along the segment so the marker sits exactly on the polyline.
        LatLng a = points.get(seg);
        LatLng b = points.get(seg + 1);
        double snappedLat = a.lat() + t * (b.lat() - a.lat());
        double snappedLng = a.lng() + t * (b.lng() - a.lng());

        return new SnappedPosition(
                snappedLat,
                snappedLng,
                progress,
                (int) Math.round(remaining),
                (int) Math.round(durationSeconds * (1.0 - progress)));
    }

    private static List<Double> cumulativeMeters(List<LatLng> points, double latScale, double lngScale) {
        List<Double> cumulative = new ArrayList<>(points.size());
        cumulative.add(0.0);
        for (int i = 1; i < points.size(); i++) {
            LatLng a = points.get(i - 1);
            LatLng b = points.get(i);
            double dx = (b.lng() - a.lng()) * lngScale;
            double dy = (b.lat() - a.lat()) * latScale;
            cumulative.add(cumulative.get(i - 1) + Math.sqrt(dx * dx + dy * dy));
        }
        return cumulative;
    }
}
