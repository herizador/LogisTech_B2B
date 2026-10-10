package com.logistech.fms.routing;

/** Distancia Haversine convertida a segundos asumiendo 50 km/h (fase 1; fase 2: OSRM). */
public final class HaversineDrivingTime {

  public static final int AVERAGE_SPEED_KMPH = 50;
  private static final int EARTH_RADIUS_IN_M = 6371000;

  private HaversineDrivingTime() {
  }

  public static long calculate(RoutingLocation from, RoutingLocation to) {
    if (from == to) {
      return 0L;
    }
    double lat1 = Math.toRadians(from.getLatitude());
    double lon1 = Math.toRadians(from.getLongitude());
    double lat2 = Math.toRadians(to.getLatitude());
    double lon2 = Math.toRadians(to.getLongitude());
    double dLat = lat2 - lat1;
    double dLon = lon2 - lon1;
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    long meters = Math.round(2 * EARTH_RADIUS_IN_M * Math.asin(Math.sqrt(a)));
    return Math.round((double) meters / AVERAGE_SPEED_KMPH * 3.6);
  }

  /** Km estimados a partir de segundos de conducción (misma hipótesis de 50 km/h). */
  public static double secondsToKm(long seconds) {
    return seconds * AVERAGE_SPEED_KMPH / 3600.0;
  }
}
