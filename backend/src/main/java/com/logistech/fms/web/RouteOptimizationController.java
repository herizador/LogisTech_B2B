package com.logistech.fms.web;

import com.logistech.fms.albaran.DeliveryNotePdf;
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
import com.logistech.fms.web.dto.RouteVerifyDto;
import com.logistech.fms.web.dto.StopDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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

  @Value("${app.public-base-url:https://logistech-b2b.onrender.com}")
  private String publicBaseUrl;

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
  @Transactional(readOnly = true)
  public List<RouteSummaryDto> list(@PathVariable UUID companyId) {    if (!companies.existsById(companyId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada");
    }
    return routes.findByCompanyIdOrderByDateDesc(companyId).stream()
        .map(this::toSummary)
        .toList();
  }

  @GetMapping("/{routeId}")
  @Transactional(readOnly = true)
  public RouteDetailDto detail(@PathVariable UUID companyId, @PathVariable UUID routeId) {
    DeliveryRoute route = routes.findById(routeId)
        .filter(r -> r.getCompany().getId().equals(companyId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no encontrada"));
    String driverName = route.getDriver() == null ? "Sin asignar" : route.getDriver().getFullName();
    List<StopDto> stopDtos = stops.findByRouteIdOrderBySeqAsc(routeId).stream()
        .map(s -> {
          var loc = s.getDeliveryOrder().getLocation();
          return new StopDto(s.getSeq(),
              s.getDeliveryOrder().getId(),
              s.getDeliveryOrder().getCustomerName(),
              s.getDeliveryOrder().getAddress(),
              loc == null ? null : loc.getY(),
              loc == null ? null : loc.getX(),
              s.getEta(),
              s.getDeliveredAt());
        })
        .toList();
    return new RouteDetailDto(route.getId(), route.getDate().toString(), driverName,
        route.getTotalKm(), stopDtos);
  }

  /** Datos públicos mínimos para verificar un albarán escaneando su QR (sin login). */
  @GetMapping("/verify/{routeId}")
  @Transactional(readOnly = true)
  public RouteVerifyDto verify(@PathVariable UUID companyId, @PathVariable UUID routeId) {
    DeliveryRoute route = routes.findById(routeId)
        .filter(r -> r.getCompany().getId().equals(companyId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no encontrada"));
    var routeStops = stops.findByRouteIdOrderBySeqAsc(routeId);
    long delivered = routeStops.stream().filter(s -> s.getDeliveredAt() != null).count();
    String driverName = route.getDriver() == null ? "Sin asignar" : route.getDriver().getFullName();
    return new RouteVerifyDto(route.getId(), route.getCompany().getName(),
        route.getDate().toString(), driverName, route.getTotalKm(),
        routeStops.size(), (int) delivered);
  }

  /** Descarga el albarán PDF de una ruta (requiere login). */
  @GetMapping(value = "/{routeId}/albaran.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> albaran(@PathVariable UUID companyId, @PathVariable UUID routeId) {
    DeliveryRoute route = routes.findById(routeId)
        .filter(r -> r.getCompany().getId().equals(companyId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ruta no encontrada"));
    DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm");
    List<DeliveryNotePdf.StopLine> lines = stops.findByRouteIdOrderBySeqAsc(routeId).stream()
        .map(s -> new DeliveryNotePdf.StopLine(s.getSeq(),
            s.getDeliveryOrder().getCustomerName(),
            s.getDeliveryOrder().getAddress(),
            s.getEta() == null ? "-" : s.getEta().atZone(java.time.ZoneOffset.UTC).format(time),
            s.getDeliveredAt() != null))
        .toList();
    String driverName = route.getDriver() == null ? "Sin asignar" : route.getDriver().getFullName();
    var doc = new DeliveryNotePdf.RouteDoc(route.getCompany().getName(),
        route.getDate().toString(), driverName,
        route.getTotalKm() == null ? "-" : route.getTotalKm().toString(),
        lines, publicBaseUrl + "/api/companies/" + companyId + "/routes/verify/" + routeId);
    byte[] pdf = DeliveryNotePdf.generate(doc);
    return ResponseEntity.ok()
        .header("Content-Disposition",
            "attachment; filename=\"albaran-" + routeId.toString().substring(0, 8) + ".pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
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
