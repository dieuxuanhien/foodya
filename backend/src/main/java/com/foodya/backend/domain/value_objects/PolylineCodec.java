package com.foodya.backend.domain.value_objects;

import java.util.ArrayList;
import java.util.List;

/** Google encoded polyline (precision 5) encoder/decoder. */
public final class PolylineCodec {

    private PolylineCodec() {}

    public static List<LatLng> decode(String encoded) {
        List<LatLng> points = new ArrayList<>();
        if (encoded == null) return points;
        int index = 0, lat = 0, lng = 0;
        while (index < encoded.length()) {
            int[] r = readValue(encoded, index);
            lat += r[0];
            index = r[1];
            r = readValue(encoded, index);
            lng += r[0];
            index = r[1];
            points.add(new LatLng(lat / 1e5, lng / 1e5));
        }
        return points;
    }

    public static String encode(List<LatLng> points) {
        StringBuilder sb = new StringBuilder();
        long prevLat = 0, prevLng = 0;
        for (LatLng p : points) {
            long lat = Math.round(p.lat() * 1e5);
            long lng = Math.round(p.lng() * 1e5);
            writeValue(sb, lat - prevLat);
            writeValue(sb, lng - prevLng);
            prevLat = lat;
            prevLng = lng;
        }
        return sb.toString();
    }

    private static int[] readValue(String s, int index) {
        int result = 0, shift = 0, b;
        do {
            if (index >= s.length()) throw new IllegalArgumentException("Malformed polyline");
            b = s.charAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        return new int[]{(result & 1) != 0 ? ~(result >> 1) : (result >> 1), index};
    }

    private static void writeValue(StringBuilder sb, long value) {
        long v = value < 0 ? ~(value << 1) : (value << 1);
        while (v >= 0x20) {
            sb.append((char) ((0x20 | (v & 0x1f)) + 63));
            v >>= 5;
        }
        sb.append((char) (v + 63));
    }
}
