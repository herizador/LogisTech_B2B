package com.logistech.fms.routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Punto geográfico con matrices de tiempo y distancia precalculadas. */
public class RoutingLocation {

  private double latitude;
  private double longitude;
  private Map<RoutingLocation, Long> drivingTimeSeconds;
  private Map<RoutingLocation, Double> distanceMeters;

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

  public void setDistanceMeters(Map<RoutingLocation, Double> distanceMeters) {
    this.distanceMeters = distanceMeters;
  }

  public long getDrivingTimeTo(RoutingLocation other) {
    return drivingTimeSeconds.get(other);
  }

  public double getDistanceTo(RoutingLocation other) {
    return distanceMeters.get(other);
  }

  /** Rellena ambas matrices con Haversine (reserva si OSRM falla). */
  public static void initHaversine(List<RoutingLocation> locations) {
    for (RoutingLocation from : locations) {
      Map<RoutingLocation, Long> times = new HashMap<>();
      Map<RoutingLocation, Double> distances = new HashMap<>();
      for (RoutingLocation to : locations) {
        times.put(to, HaversineDrivingTime.calculate(from, to));
        distances.put(to, HaversineDrivingTime.meters(from, to));
      }
      from.setDrivingTimeSeconds(times);
      from.setDistanceMeters(distances);
    }
  }

  /** Rellena ambas matrices con datos reales de OSRM. */
  public static void initFromMatrices(List<RoutingLocation> locations,
      long[][] durationsSeconds, double[][] distancesMeters) {
    for (int i = 0; i < locations.size(); i++) {
      Map<RoutingLocation, Long> times = new HashMap<>();
      Map<RoutingLocation, Double> distances = new HashMap<>();
      for (int j = 0; j < locations.size(); j++) {
        times.put(locations.get(j), durationsSeconds[i][j]);
        distances.put(locations.get(j), distancesMeters[i][j]);
      }
      locations.get(i).setDrivingTimeSeconds(times);
      locations.get(i).setDistanceMeters(distances);
    }
  }

  @Override
  public String toString() {
    return latitude + "," + longitude;
  }
}
