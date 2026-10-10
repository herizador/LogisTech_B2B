package com.logistech.fms.repository;

import com.logistech.fms.domain.CustomerOrder;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {

  List<CustomerOrder> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
