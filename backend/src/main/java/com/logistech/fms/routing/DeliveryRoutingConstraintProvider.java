package com.logistech.fms.routing;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;

/**
 * Restricciones del reparto:
 * - Duras: no superar capacidad; no terminar fuera de la ventana del cliente.
 * - Media: asignar el máximo de visitas posible.
 * - Blanda: minimizar tiempo de conducción.
 */
public class DeliveryRoutingConstraintProvider implements ConstraintProvider {

  public static final String VEHICLE_CAPACITY = "vehicleCapacity";
  public static final String SERVICE_FINISHED_AFTER_MAX_END_TIME = "serviceFinishedAfterMaxEndTime";
  public static final String MAXIMIZE_VISITS_ASSIGNED = "maximizeVisitsAssigned";
  public static final String MINIMIZE_TRAVEL_TIME = "minimizeTravelTime";

  @Override
  public Constraint[] defineConstraints(ConstraintFactory factory) {
    return new Constraint[] {
        vehicleCapacity(factory),
        serviceFinishedAfterMaxEndTime(factory),
        maximizeVisitsAssigned(factory),
        minimizeTravelTime(factory)
    };
  }

  protected Constraint vehicleCapacity(ConstraintFactory factory) {
    return factory.forEach(DeliveryVehicle.class)
        .filter(vehicle -> vehicle.getTotalDemandKg() > vehicle.getCapacityKg())
        .penalize(HardMediumSoftScore.ONE_HARD,
            vehicle -> vehicle.getTotalDemandKg() - vehicle.getCapacityKg())
        .asConstraint(VEHICLE_CAPACITY);
  }

  protected Constraint serviceFinishedAfterMaxEndTime(ConstraintFactory factory) {
    return factory.forEach(DeliveryVisit.class)
        .filter(DeliveryVisit::isServiceFinishedAfterMaxEndTime)
        .penalize(HardMediumSoftScore.ONE_HARD,
            DeliveryVisit::getServiceFinishedDelayInMinutes)
        .asConstraint(SERVICE_FINISHED_AFTER_MAX_END_TIME);
  }

  protected Constraint maximizeVisitsAssigned(ConstraintFactory factory) {
    return factory.forEachIncludingUnassigned(DeliveryVisit.class)
        .filter(visit -> visit.getVehicle() == null)
        .penalize(HardMediumSoftScore.ONE_MEDIUM,
            visit -> (int) visit.getServiceDuration().toMinutes())
        .asConstraint(MAXIMIZE_VISITS_ASSIGNED);
  }

  protected Constraint minimizeTravelTime(ConstraintFactory factory) {
    return factory.forEach(DeliveryVehicle.class)
        .penalize(HardMediumSoftScore.ONE_SOFT,
            DeliveryVehicle::getTotalDrivingTimeSeconds)
        .asConstraint(MINIMIZE_TRAVEL_TIME);
  }
}
