package com.logistech.fms.routing;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Un pedido a entregar (entidad de planificación, no persistente). */
@PlanningEntity
public class DeliveryVisit {

  @PlanningId
  private String id;
  private UUID orderId;
  private String customerName;
  private RoutingLocation location;
  private int demandKg;
  private LocalDateTime minStartTime;
  private LocalDateTime maxEndTime;
  private Duration serviceDuration;

  @InverseRelationShadowVariable(sourceVariableName = "visits")
  private DeliveryVehicle vehicle;
  @PreviousElementShadowVariable(sourceVariableName = "visits")
  private DeliveryVisit previousVisit;
  @ShadowVariable(supplierName = "arrivalTimeSupplier")
  private LocalDateTime arrivalTime;

  public DeliveryVisit() {
  }

  public DeliveryVisit(UUID orderId, String customerName, RoutingLocation location, int demandKg,
      LocalDateTime minStartTime, LocalDateTime maxEndTime, Duration serviceDuration) {
    this.id = orderId.toString();
    this.orderId = orderId;
    this.customerName = customerName;
    this.location = location;
    this.demandKg = demandKg;
    this.minStartTime = minStartTime;
    this.maxEndTime = maxEndTime;
    this.serviceDuration = serviceDuration;
  }

  public String getId() {
    return id;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public RoutingLocation getLocation() {
    return location;
  }

  public int getDemandKg() {
    return demandKg;
  }

  public LocalDateTime getMinStartTime() {
    return minStartTime;
  }

  public LocalDateTime getMaxEndTime() {
    return maxEndTime;
  }

  public Duration getServiceDuration() {
    return serviceDuration;
  }

  public DeliveryVehicle getVehicle() {
    return vehicle;
  }

  public void setVehicle(DeliveryVehicle vehicle) {
    this.vehicle = vehicle;
  }

  public DeliveryVisit getPreviousVisit() {
    return previousVisit;
  }

  public void setPreviousVisit(DeliveryVisit previousVisit) {
    this.previousVisit = previousVisit;
  }

  public LocalDateTime getArrivalTime() {
    return arrivalTime;
  }

  public void setArrivalTime(LocalDateTime arrivalTime) {
    this.arrivalTime = arrivalTime;
  }

  @SuppressWarnings("unused")
  @ShadowSources({"vehicle", "previousVisit.arrivalTime"})
  public LocalDateTime arrivalTimeSupplier() {
    if (previousVisit == null && vehicle == null) {
      return null;
    }
    LocalDateTime departureTime = previousVisit == null ? vehicle.getDepartureTime() : previousVisit.getDepartureTime();
    return departureTime != null ? departureTime.plusSeconds(getDrivingTimeSecondsFromPrevious()) : null;
  }

  public LocalDateTime getDepartureTime() {
    if (arrivalTime == null) {
      return null;
    }
    return getStartServiceTime().plus(serviceDuration);
  }

  public LocalDateTime getStartServiceTime() {
    if (arrivalTime == null) {
      return null;
    }
    return arrivalTime.isBefore(minStartTime) ? minStartTime : arrivalTime;
  }

  public boolean isServiceFinishedAfterMaxEndTime() {
    return arrivalTime != null && arrivalTime.plus(serviceDuration).isAfter(maxEndTime);
  }

  public long getServiceFinishedDelayInMinutes() {
    if (arrivalTime == null) {
      return 0;
    }
    return roundUpMinutes(Duration.between(maxEndTime, arrivalTime.plus(serviceDuration)));
  }

  private static long roundUpMinutes(Duration duration) {
    if (duration.isNegative() || duration.isZero()) {
      return 0;
    }
    var remainder = duration.minus(duration.truncatedTo(ChronoUnit.MINUTES));
    return remainder.equals(Duration.ZERO) ? duration.toMinutes() : duration.toMinutes() + 1;
  }

  public long getDrivingTimeSecondsFromPrevious() {
    if (vehicle == null) {
      throw new IllegalStateException("Shadow variables aún no inicializadas.");
    }
    if (previousVisit == null) {
      return vehicle.getHomeLocation().getDrivingTimeTo(location);
    }
    return previousVisit.getLocation().getDrivingTimeTo(location);
  }

  @Override
  public String toString() {
    return id;
  }
}
