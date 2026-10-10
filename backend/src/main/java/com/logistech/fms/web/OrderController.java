package com.logistech.fms.web;

import com.logistech.fms.domain.Company;
import com.logistech.fms.domain.CustomerOrder;
import com.logistech.fms.repository.CompanyRepository;
import com.logistech.fms.repository.CustomerOrderRepository;
import com.logistech.fms.web.dto.OrderRequest;
import com.logistech.fms.web.dto.OrderResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
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
@RequestMapping("/api/companies/{companyId}/orders")
public class OrderController {

  private final CompanyRepository companies;
  private final CustomerOrderRepository orders;
  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

  public OrderController(CompanyRepository companies, CustomerOrderRepository orders) {
    this.companies = companies;
    this.orders = orders;
  }

  @GetMapping
  public List<OrderResponse> list(@PathVariable UUID companyId) {
    requireCompany(companyId);
    return orders.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
        .map(this::toResponse)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse create(@PathVariable UUID companyId, @Valid @RequestBody OrderRequest request) {
    Company company = requireCompany(companyId);
    CustomerOrder order = new CustomerOrder();
    order.setCompany(company);
    order.setCustomerName(request.customerName());
    order.setAddress(request.address());
    if (request.lat() != null && request.lng() != null) {
      order.setLocation(geometryFactory.createPoint(new Coordinate(request.lng(), request.lat())));
    }
    order.setWeightKg(request.weightKg());
    order.setWindowStart(request.windowStart());
    order.setWindowEnd(request.windowEnd());
    return toResponse(orders.save(order));
  }

  private Company requireCompany(UUID companyId) {
    return companies.findById(companyId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
  }

  private OrderResponse toResponse(CustomerOrder order) {
    Double lat = order.getLocation() != null ? order.getLocation().getY() : null;
    Double lng = order.getLocation() != null ? order.getLocation().getX() : null;
    return new OrderResponse(
        order.getId(),
        order.getCustomerName(),
        order.getAddress(),
        lat,
        lng,
        order.getWeightKg(),
        order.getStatus(),
        order.getWindowStart(),
        order.getWindowEnd(),
        order.getCreatedAt());
  }
}
