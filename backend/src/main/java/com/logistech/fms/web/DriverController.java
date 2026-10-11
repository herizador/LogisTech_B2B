package com.logistech.fms.web;

import com.logistech.fms.domain.Company;
import com.logistech.fms.domain.Driver;
import com.logistech.fms.repository.CompanyRepository;
import com.logistech.fms.repository.DriverRepository;
import com.logistech.fms.web.dto.DriverRequest;
import com.logistech.fms.web.dto.DriverResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/companies/{companyId}/drivers")
public class DriverController {

  private final CompanyRepository companies;
  private final DriverRepository drivers;

  public DriverController(CompanyRepository companies, DriverRepository drivers) {
    this.companies = companies;
    this.drivers = drivers;
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
}
