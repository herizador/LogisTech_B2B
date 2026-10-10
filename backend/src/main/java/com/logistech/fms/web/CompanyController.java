package com.logistech.fms.web;

import com.logistech.fms.repository.CompanyRepository;
import com.logistech.fms.web.dto.CompanyResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

  private final CompanyRepository companies;

  public CompanyController(CompanyRepository companies) {
    this.companies = companies;
  }

  @GetMapping
  public List<CompanyResponse> list() {
    return companies.findAll().stream()
        .map(c -> new CompanyResponse(c.getId(), c.getName()))
        .toList();
  }
}
