package com.logistech.fms.repository;

import com.logistech.fms.domain.DeliveryRoute;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRouteRepository extends JpaRepository<DeliveryRoute, UUID> {

  List<DeliveryRoute> findByCompanyIdAndDate(UUID companyId, LocalDate date);
}
