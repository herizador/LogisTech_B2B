package com.logistech.fms.repository;

import com.logistech.fms.domain.Driver;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, UUID> {

  List<Driver> findByCompanyId(UUID companyId);
}
