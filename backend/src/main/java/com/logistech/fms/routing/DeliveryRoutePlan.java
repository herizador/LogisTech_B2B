package com.logistech.fms.routing;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import java.util.List;

/** Problema de rutas de un día: vehículos + visitas pendientes. */
@PlanningSolution
public class DeliveryRoutePlan {

  private String name;

  @PlanningEntityCollectionProperty
  private List<DeliveryVehicle> vehicles;

  @PlanningEntityCollectionProperty
  @ValueRangeProvider
  private List<DeliveryVisit> visits;

  @PlanningScore
  private HardMediumSoftScore score;

  public DeliveryRoutePlan() {
  }

  public DeliveryRoutePlan(String name, List<DeliveryVehicle> vehicles, List<DeliveryVisit> visits) {
    this.name = name;
    this.vehicles = vehicles;
    this.visits = visits;
  }

  public String getName() {
    return name;
  }

  public List<DeliveryVehicle> getVehicles() {
    return vehicles;
  }

  public List<DeliveryVisit> getVisits() {
    return visits;
  }

  public HardMediumSoftScore getScore() {
    return score;
  }

  public void setScore(HardMediumSoftScore score) {
    this.score = score;
  }

  public long getTotalDrivingTimeSeconds() {
    return vehicles == null ? 0 : vehicles.stream().mapToLong(DeliveryVehicle::getTotalDrivingTimeSeconds).sum();
  }
}
