package com.logistech.fms.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

  @GetMapping("/")
  public Map<String, String> root() {
    return Map.of(
        "service", "logistech-backend",
        "health", "/api/health",
        "docs", "/swagger-ui.html");
  }
}
