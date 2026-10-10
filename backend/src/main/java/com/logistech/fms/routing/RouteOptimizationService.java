package com.logistech.fms.routing;

import ai.timefold.solver.core.api.solver.SolverJob;
import ai.timefold.solver.core.api.solver.SolverManager;
import com.logistech.fms.domain.Company;import com.logistech.fms.domain.CustomerOrder;
import com.logistech.fms.domain.DeliveryRoute;
import com.logistech.fms.domain.Driver;
import com.logistech.fms.domain.RouteStop;
import com.logistech.fms.repository.CompanyRepository;
import com.logistech.fms.repository.CustomerOrderRepository;
import com.logistech.fms.repository.DeliveryRouteRepository;
import com.logistech.fms.repository.DriverRepository;
import com.logistech.fms.repository.RouteStopRepository;
import com.logistech.fms.web.dto.OptimizeResponse;
import com.logistech.fms.web.dto.RouteResultDto;
import com.logistech.fms.web.dto.SkippedOrderDto;
import com.logistech.fms.web.dto.StopDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RouteOptimizationService {

  private static final Logger log = LoggerFactory.getLogger(RouteOptimizationService.class);
  private static final Duration SERVICE_DURATION = Duration.ofMinutes(5);
  private static final int DEFAULT_CAPACITY_KG = 1000;

  private final CompanyRepository companies;
  private final DriverRepository drivers;
  private final CustomerOrderRepository orders;
  private final DeliveryRouteRepository routes;
  private final RouteStopRepository stops;
  private final SolverManager<DeliveryRoutePlan> solverManager;
  private final OsrmClient osrm;

  public RouteOptimizationService(CompanyRepository companies, DriverRepository drivers,
      CustomerOrderRepository orders, DeliveryRouteRepository routes, RouteStopRepository stops,
      SolverManager<DeliveryRoutePlan> solverManager, OsrmClient osrm) {
    this.companies = companies;
    this.drivers = drivers;
    this.orders = orders;
    this.routes = routes;
    this.stops = stops;
    this.solverManager = solverManager;
    this.osrm = osrm;
  }

  @Transactional
  public OptimizeResponse optimize(UUID companyId, LocalDate date, double depotLat, double depotLng) {
    Company company = companies.findById(companyId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));

    List<CustomerOrder> pending = orders.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
        .filter(o -> "pending".equals(o.getStatus()))
        .toList();

    List<SkippedOrderDto> skipped = pending.stream()
        .filter(o -> o.getLocation() == null)
        .map(o -> new SkippedOrderDto(o.getId(), o.getCustomerName(), "sin coordenadas"))
        .toList();

    List<CustomerOrder> solvable = pending.stream()
        .filter(o -> o.getLocation() != null)
        .toList();

    if (solvable.isEmpty()) {
      return new OptimizeResponse(date.toString(), 0, 0, List.of(), skipped);
    }

    LocalDateTime departure = date.atTime(8, 0);
    RoutingLocation depot = new RoutingLocation(depotLat, depotLng);

    List<DeliveryVehicle> vehicles = buildVehicles(companyId, depot, departure);
    List<DeliveryVisit> visits = solvable.stream()
        .map(o -> toVisit(o, departure))
        .toList();

    List<RoutingLocation> allLocations = new ArrayList<>();
    allLocations.add(depot);
    visits.forEach(v -> allLocations.add(v.getLocation()));
    loadRoadMatrices(allLocations);

    DeliveryRoutePlan problem = new DeliveryRoutePlan("rutas-" + date, vehicles, visits);
    DeliveryRoutePlan solved = solve(problem);

    Map<UUID, CustomerOrder> ordersById = solvable.stream()
        .collect(Collectors.toMap(CustomerOrder::getId, Function.identity()));

    List<RouteResultDto> routeDtos = new ArrayList<>();
    for (DeliveryVehicle vehicle : solved.getVehicles()) {
      if (vehicle.getVisits().isEmpty()) {
        continue;
      }
      Driver driver = vehicle.getDriverId() == null ? null
          : drivers.findById(vehicle.getDriverId()).orElse(null);
      DeliveryRoute route = new DeliveryRoute();
      route.setCompany(company);
      route.setDriver(driver);
      route.setDate(date);
      long drivingSeconds = vehicle.getTotalDrivingTimeSeconds();
      route.setTotalKm(BigDecimal.valueOf(vehicle.getTotalDistanceMeters() / 1000.0)
          .setScale(2, RoundingMode.HALF_UP));
      routes.save(route);

      List<RoutingLocation> ordered = new ArrayList<>();
      ordered.add(depot);
      vehicle.getVisits().forEach(v -> ordered.add(v.getLocation()));
      ordered.add(depot);
      List<List<Double>> geometry = loadGeometry(ordered);

      List<StopDto> stopDtos = new ArrayList<>();
      int seq = 1;
      for (DeliveryVisit visit : vehicle.getVisits()) {
        CustomerOrder order = ordersById.get(visit.getOrderId());
        RouteStop stop = new RouteStop();
        stop.setRoute(route);
        stop.setDeliveryOrder(order);
        stop.setSeq(seq);
        if (visit.getArrivalTime() != null) {
          stop.setDeliveredAt(null);
          stopDtos.add(new StopDto(seq, order.getId(), order.getCustomerName(), order.getAddress(),
              visit.getArrivalTime().atZone(ZoneOffset.UTC).toInstant()));
          stop.setEta(visit.getArrivalTime().atZone(ZoneOffset.UTC).toInstant());
        } else {
          stopDtos.add(new StopDto(seq, order.getId(), order.getCustomerName(), order.getAddress(), null));
        }
        stops.save(stop);
        order.setStatus("assigned");
        orders.save(order);
        seq++;
      }
      routeDtos.add(new RouteResultDto(route.getId(),
          driver == null ? "Sin asignar" : driver.getFullName(),
          route.getTotalKm(), drivingSeconds / 60, stopDtos, geometry));
    }

    double totalKm = routeDtos.stream().mapToDouble(r -> r.totalKm().doubleValue()).sum();
    long totalMinutes = routeDtos.stream().mapToLong(RouteResultDto::drivingMinutes).sum();
    return new OptimizeResponse(date.toString(), totalKm, totalMinutes, routeDtos, skipped);
  }

  private List<DeliveryVehicle> buildVehicles(UUID companyId, RoutingLocation depot, LocalDateTime departure) {
    List<Driver> companyDrivers = drivers.findByCompanyId(companyId);
    if (companyDrivers.isEmpty()) {
      return List.of(new DeliveryVehicle("general", null, "General", DEFAULT_CAPACITY_KG, depot, departure));
    }
    return companyDrivers.stream()
        .map(d -> new DeliveryVehicle(d.getId().toString(), d.getId(), d.getFullName(),
            d.getVehicleCapacityKg() == null ? DEFAULT_CAPACITY_KG : d.getVehicleCapacityKg().intValue(),
            depot, departure))
        .toList();
  }

  private DeliveryVisit toVisit(CustomerOrder order, LocalDateTime departure) {
    RoutingLocation location = new RoutingLocation(
        order.getLocation().getY(), order.getLocation().getX());
    int demand = order.getWeightKg() == null ? 0 : order.getWeightKg().intValue();
    LocalDateTime minStart = order.getWindowStart() == null ? departure
        : LocalDateTime.ofInstant(order.getWindowStart(), ZoneOffset.UTC);
    LocalDateTime maxEnd = order.getWindowEnd() == null ? departure.plusHours(12)
        : LocalDateTime.ofInstant(order.getWindowEnd(), ZoneOffset.UTC);
    return new DeliveryVisit(order.getId(), order.getCustomerName(), location, demand,
        minStart, maxEnd, SERVICE_DURATION);
  }

  private void loadRoadMatrices(List<RoutingLocation> allLocations) {
    try {
      OsrmClient.Matrices matrices = osrm.table(allLocations);
      RoutingLocation.initFromMatrices(allLocations,
          matrices.durationsSeconds(), matrices.distancesMeters());
      log.info("Matriz OSRM cargada para {} ubicaciones", allLocations.size());
    } catch (Exception e) {
      log.warn("OSRM no disponible, usando Haversine: {}", e.getMessage());
      RoutingLocation.initHaversine(allLocations);
    }
  }

  private List<List<Double>> loadGeometry(List<RoutingLocation> ordered) {
    try {
      return osrm.routeGeometry(ordered);
    } catch (Exception e) {
      log.warn("Sin geometría OSRM, línea recta: {}", e.getMessage());
      return null;
    }
  }

  private DeliveryRoutePlan solve(DeliveryRoutePlan problem) {    try {
      SolverJob<DeliveryRoutePlan> job = solverManager.solve(UUID.randomUUID(), problem);
      return job.getFinalBestSolution();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Optimización interrumpida");
    } catch (ExecutionException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Fallo la optimización: " + e.getCause().getMessage());
    }
  }
}
