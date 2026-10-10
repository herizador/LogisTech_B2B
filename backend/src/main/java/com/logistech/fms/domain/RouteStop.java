package com.logistech.fms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "route_stops")
public class RouteStop {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "route_id", nullable = false)
  private DeliveryRoute route;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private CustomerOrder deliveryOrder;

  @Column(nullable = false)
  private Integer seq;

  private Instant eta;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public DeliveryRoute getRoute() {
    return route;
  }

  public void setRoute(DeliveryRoute route) {
    this.route = route;
  }

  public CustomerOrder getDeliveryOrder() {
    return deliveryOrder;
  }

  public void setDeliveryOrder(CustomerOrder deliveryOrder) {
    this.deliveryOrder = deliveryOrder;
  }

  public Integer getSeq() {
    return seq;
  }

  public void setSeq(Integer seq) {
    this.seq = seq;
  }

  public Instant getEta() {
    return eta;
  }

  public void setEta(Instant eta) {
    this.eta = eta;
  }

  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  public void setDeliveredAt(Instant deliveredAt) {
    this.deliveredAt = deliveredAt;
  }
}
