package com.logistech.fms.routing;

import java.util.HashMap;
import java.util.Map;

/** Punto geográfico con matriz de tiempos de conducción precalculada (Haversine, 50 km/h). */
public class RoutingLocation {

  private double latitude;
  private double longitude;
  private Map<RoutingLocation, Long> drivingTimeSeconds;

  public RoutingLocation() {
  }

  public RoutingLocation(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setDrivingTimeSeconds(Map<RoutingLocation, Long> drivingTimeSeconds) {
    this.drivingTimeSeconds = drivingTimeSeconds;
  }

  public long getDrivingTimeTo(RoutingLocation other) {
    return drivingTimeSeconds.get(other);
  }

  public static void initDrivingTimeMaps(java.util.List<RoutingLocation> locations) {
    for (RoutingLocation from : locations) {
      Map<RoutingLocation, Long> map = new HashMap<>();
      for (RoutingLocation to : locations) {
        map.put(to, HaversineDrivingTime.calculate(from, to));
      }
      from.setDrivingTimeSeconds(map);
    }
  }

  @Override
  public String toString() {
    return latitude + "," + longitude;
  }
}
