package com.example.springjwt.util;

public class DistanceUtil {

    /**
     * 하버사인 공식으로 두 좌표 간 거리(km) 계산
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // 지구 반지름 km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

}