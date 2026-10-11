package com.logistech.fms.web;

import com.logistech.fms.domain.DeliveryRoute;
import com.logistech.fms.repository.CompanyRepository;
import com.logistech.fms.repository.CustomerOrderRepository;
import com.logistech.fms.repository.DeliveryRouteRepository;
import com.logistech.fms.repository.RouteStopRepository;
import com.logistech.fms.routing.RouteOptimizationService;
import com.logistech.fms.web.dto.OptimizeRequest;
import com.logistech.fms.web.dto.OptimizeResponse;
import com.logistech.fms.web.dto.RouteDetailDto;
import com.logistech.fms.web.dto.RouteSummaryDto;
import com.logistech.fms.web.dto.StopDto;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/companies/{companyId}/routes")
public class RouteOptimizationController {

  private final RouteOptimizationService optimizationService;
  private final CompanyRepository companies;
  private final CustomerOrderRepository orders;
  private final DeliveryRouteRepository routes;
  private final RouteStopRepository stops;

  public RouteOptimizationController(RouteOptimizationService optimizationService,
      CompanyRepository companies, CustomerOrderRepository orders,
      DeliveryRouteRepository routes, RouteStopRepository stops) {
    this.optimizationService = optimizationService;
    this.companies = companies;
    this.orders = orders;
    this.routes = routes;
    this.stops = stops;
  }

  @PostMapping("/optimize")
  public OptimizeResponse optimize(@PathVariable UUID companyId,
      @RequestBody(required = false) OptimizeRequest request) {
    LocalDate date = request != null && request.date() != null && !request.date().isBlank()
        ? LocalDate.parse(request.date())
        : LocalDate.now();
    double depotLat = request != null && request.depotLat() != null ? request.depotLat() : 41.648;
    double depotLng = request != null && request.depotLng() != null ? request.depotLng() : -0.877;
    return optimizationService.optimize(companyId, date, depotLat, depotLng);
  }

  @GetMapping
  public List<RouteSummaryDto> list(@PathVariable UUID companyId) {    if (!companies.existsById(companyId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada");
    }
    return routes.findByCompanyIdOrderByDateDesc(companyId).stream()
        .map(this::toSummary)
        .toList();
  }

  @GetMapping("/{routeId}")
  public RouteDetailDto detail(@PathVariable UUID companyId, @PathVariable UUID routeId) {
    DeliveryRoute route = routes.findById(routeId)
        .filter(r -> r.getCompany().getId().equals(companyId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no encontrada"));
    String driverName = route.getDriver() == null ? "Sin asignar" : route.getDriver().getFullName();
    List<StopDto> stopDtos = stops.findByRouteIdOrderBySeqAsc(routeId).stream()
        .map(s -> new StopDto(s.getSeq(),
            s.getDeliveryOrder().getId(),
            s.getDeliveryOrder().getCustomerName(),
            s.getDeliveryOrder().getAddress(),
            s.getEta(),
            s.getDeliveredAt()))
        .toList();
    return new RouteDetailDto(route.getId(), route.getDate().toString(), driverName,
        route.getTotalKm(), stopDtos);
  }

  private RouteSummaryDto toSummary(DeliveryRoute route) {
    String driverName = route.getDriver() == null ? "Sin asignar" : route.getDriver().getFullName();
    return new RouteSummaryDto(route.getId(), route.getDate(), driverName,
        route.getTotalKm(), stops.countByRouteId(route.getId()));
  }

  /**
   * Reinicia un día: borra sus rutas y devuelve los pedidos asignados a pendiente,
   * para poder re-optimizar (útil tras corregir coordenadas).
   */
  @PostMapping("/reset")
  public java.util.Map<String, Object> resetDay(@PathVariable UUID companyId,
      @RequestBody(required = false) java.util.Map<String, String> body) {
    if (!companies.existsById(companyId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada");
    }
    String ds = body != null ? body.get("date") : null;
    LocalDate date = (ds != null && !ds.isBlank()) ? LocalDate.parse(ds) : LocalDate.now();

    var dayRoutes = routes.findByCompanyIdAndDate(companyId, date);
    long stopsDeleted = 0;
    for (DeliveryRoute route : dayRoutes) {
      var dayStops = stops.findByRouteIdOrderBySeqAsc(route.getId());
      stopsDeleted += dayStops.size();
      stops.deleteAll(dayStops);
      routes.delete(route);
    }

    var assigned = orders.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
        .filter(o -> "assigned".equals(o.getStatus()))
        .toList();
    assigned.forEach(o -> o.setStatus("pending"));
    orders.saveAll(assigned);

    return java.util.Map.of(
        "date", date.toString(),
        "routesDeleted", dayRoutes.size(),
        "stopsDeleted", stopsDeleted,
        "ordersReset", assigned.size());
  }
}
