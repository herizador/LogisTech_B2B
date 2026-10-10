package com.logistech.fms.routing;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Una furgoneta/conductor con su secuencia de visitas (variable de planificación). */
@PlanningEntity
public class DeliveryVehicle {

  @PlanningId
  private String id;
  private UUID driverId;
  private String driverName;
  private int capacityKg;
  private RoutingLocation homeLocation;
  private LocalDateTime departureTime;

  @PlanningListVariable(allowsUnassignedValues = true)
  private List<DeliveryVisit> visits;

  public DeliveryVehicle() {
  }

  public DeliveryVehicle(String id, UUID driverId, String driverName, int capacityKg,
      RoutingLocation homeLocation, LocalDateTime departureTime) {
    this.id = id;
    this.driverId = driverId;
    this.driverName = driverName;
    this.capacityKg = capacityKg;
    this.homeLocation = homeLocation;
    this.departureTime = departureTime;
    this.visits = new ArrayList<>();
  }

  public String getId() {
    return id;
  }

  public UUID getDriverId() {
    return driverId;
  }

  public String getDriverName() {
    return driverName;
  }

  public int getCapacityKg() {
    return capacityKg;
  }

  public RoutingLocation getHomeLocation() {
    return homeLocation;
  }

  public LocalDateTime getDepartureTime() {
    return departureTime;
  }

  public List<DeliveryVisit> getVisits() {
    return visits;
  }

  public void setVisits(List<DeliveryVisit> visits) {
    this.visits = visits;
  }

  public int getTotalDemandKg() {
    int total = 0;
    for (DeliveryVisit visit : visits) {
      total += visit.getDemandKg();
    }
    return total;
  }

  public long getTotalDrivingTimeSeconds() {
    if (visits.isEmpty()) {
      return 0;
    }
    long total = 0;
    RoutingLocation previous = homeLocation;
    for (DeliveryVisit visit : visits) {
      total += previous.getDrivingTimeTo(visit.getLocation());
      previous = visit.getLocation();
    }
    total += previous.getDrivingTimeTo(homeLocation);
    return total;
  }

  public double getTotalDistanceMeters() {
    if (visits.isEmpty()) {
      return 0;
    }
    double total = 0;
    RoutingLocation previous = homeLocation;
    for (DeliveryVisit visit : visits) {
      total += previous.getDistanceTo(visit.getLocation());
      previous = visit.getLocation();
    }
    total += previous.getDistanceTo(homeLocation);
    return total;
  }

  @Override
  public String toString() {
    return id;
  }
}
