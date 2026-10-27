package com.bank.platform.suitability.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service identity for suitability. The seller journey lives on
 * /internal/v1/suitability/assessments.
 */
@RestController
@RequestMapping("/internal/v1/suitability")
public class ServiceInfoController {

  @GetMapping("/info")
  public ResponseEntity<Map<String, String>> info() {
    return ResponseEntity.ok(
        Map.of(
            "service", "suitability-service",
            "boundedContext", "7",
            "status", "IMPLEMENTED"));
  }
}
