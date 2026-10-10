package com.logistech.fms.repository;

import com.logistech.fms.domain.RouteStop;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteStopRepository extends JpaRepository<RouteStop, UUID> {

  List<RouteStop> findByRouteIdOrderBySeqAsc(UUID routeId);

  long countByRouteId(UUID routeId);
}
