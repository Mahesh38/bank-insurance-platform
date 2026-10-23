package com.bank.platform.consent.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service identity for operators. Business routes live on {@link ConsentController}. */
@RestController
@RequestMapping("/internal/v1/consent")
public class ServiceInfoController {

  @GetMapping("/info")
  public ResponseEntity<Map<String, String>> info() {
    return ResponseEntity.ok(
        Map.of(
            "service", "consent-service",
            "boundedContext", "6",
            "status", "ACTIVE"));
  }
}
