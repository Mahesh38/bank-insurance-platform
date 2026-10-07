package com.bank.workforce.bff.api;

import com.bank.workforce.bff.application.CountryCodeCatalog;
import com.bank.workforce.bff.application.CountryCodeCatalog.CountryCode;
import com.bank.workforce.bff.application.CountryCodeCatalog.MobileValidationResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReferenceDataController {

  private final CountryCodeCatalog countryCodes;

  public ReferenceDataController(CountryCodeCatalog countryCodes) {
    this.countryCodes = countryCodes;
  }

  @GetMapping("/country-codes")
  public List<CountryCode> countryCodes(HttpServletRequest request) {
    BffSessionInterceptor.requireSession(request);
    return countryCodes.list();
  }

  @PostMapping("/mobile-numbers:validate")
  public MobileValidationResult validateMobile(
      HttpServletRequest request, @Valid @RequestBody MobileValidationRequest body) {
    BffSessionInterceptor.requireSession(request);
    return countryCodes.validate(body.countryCode(), body.nationalNumber());
  }

  @GetMapping("/catalogue/product-classes")
  public ProductClassPage productClasses(HttpServletRequest request) {
    BffSessionInterceptor.requireSession(request);
    return new ProductClassPage(
        List.of(
            new ProductClass("LIFE", "TERM", "Term Life"),
            new ProductClass("LIFE", "SAVINGS", "Savings / ULIP-adjacent savings"),
            new ProductClass("LIFE", "ULIP", "ULIP")));
  }

  public record MobileValidationRequest(
      @NotBlank String countryCode, @NotBlank String nationalNumber) {}

  public record ProductClassPage(List<ProductClass> items) {}

  public record ProductClass(String lob, String productClass, String name) {}
}
