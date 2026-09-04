package {{PACKAGE}}.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Minimal scaffold endpoint proving the service boots and exposes a bank-canonical route prefix.
 * Replace with real API controllers when the bounded context is implemented.
 */
@RestController
@RequestMapping("/internal/v1/{{SERVICE_ID}}")
public class ServiceInfoController {

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> info() {
        return ResponseEntity.ok(Map.of(
            "service", "{{MODULE}}",
            "boundedContext", "{{CONTEXT_ID}}",
            "status", "SKELETON"
        ));
    }
}
