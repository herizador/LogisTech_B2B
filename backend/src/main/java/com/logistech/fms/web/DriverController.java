package com.logistech.fms.web;

import com.logistech.fms.domain.Company;
import com.logistech.fms.domain.DeliveryRoute;
import com.logistech.fms.domain.Driver;
import com.logistech.fms.repository.CompanyRepository;
import com.logistech.fms.repository.DeliveryRouteRepository;
import com.logistech.fms.repository.DriverRepository;
import com.logistech.fms.repository.RouteStopRepository;
import com.logistech.fms.web.dto.DriverRequest;
import com.logistech.fms.web.dto.DriverResponse;
import com.logistech.fms.web.dto.RouteDetailDto;
import com.logistech.fms.web.dto.StopDto;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/companies/{companyId}/drivers")
public class DriverController {

  private final CompanyRepository companies;
  private final DriverRepository drivers;
  private final DeliveryRouteRepository routes;
  private final RouteStopRepository stops;

  public DriverController(CompanyRepository companies, DriverRepository drivers,
      DeliveryRouteRepository routes, RouteStopRepository stops) {
    this.companies = companies;
    this.drivers = drivers;
    this.routes = routes;
    this.stops = stops;
  }

  @GetMapping
  public List<DriverResponse> list(@PathVariable UUID companyId) {
    requireCompany(companyId);
    return drivers.findByCompanyId(companyId).stream()
        .map(d -> new DriverResponse(d.getId(), d.getFullName(), d.getVehicleCapacityKg()))
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DriverResponse create(@PathVariable UUID companyId, @Valid @RequestBody DriverRequest request) {
    Company company = requireCompany(companyId);
    Driver driver = new Driver();
    driver.setCompany(company);
    driver.setFullName(request.fullName());
    driver.setVehicleCapacityKg(request.vehicleCapacityKg());
    drivers.save(driver);
    return new DriverResponse(driver.getId(), driver.getFullName(), driver.getVehicleCapacityKg());
  }

  private Company requireCompany(UUID companyId) {
    return companies.findById(companyId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
  }

  /** Rutas de un conductor en una fecha (para la PWA del repartidor). */
  @GetMapping("/{driverId}/routes")
  @Transactional(readOnly = true)
  public List<RouteDetailDto> routesByDriver(@PathVariable UUID companyId,
      @PathVariable UUID driverId,
      @RequestParam(required = false) String date) {
    requireCompany(companyId);
    if (!drivers.existsById(driverId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conductor no encontrado");
    }
    LocalDate day = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
    return routes.findByCompanyIdAndDate(companyId, day).stream()
        .filter(r -> r.getDriver() != null && r.getDriver().getId().equals(driverId))
        .map(this::toDetail)
        .toList();
  }

  private RouteDetailDto toDetail(DeliveryRoute route) {
    String driverName = route.getDriver() == null ? "Sin asignar" : route.getDriver().getFullName();
    List<StopDto> stopDtos = stops.findByRouteIdOrderBySeqAsc(route.getId()).stream()
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
}
