package com.bank.platform.suitability.adapter.catalog;

import com.bank.platform.suitability.config.JourneyProperties;
import com.bank.platform.suitability.domain.JourneyModel.IncomeBand;
import com.bank.platform.suitability.domain.JourneyModel.Masters;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Occupation, education and income bands from configuration. These are not the final bank masters.
 */
@Component
public class ConfiguredMasters implements Masters {

  private final Set<String> occupations;
  private final Set<String> educations;
  private final Map<String, IncomeBand> incomes;

  public ConfiguredMasters(JourneyProperties properties) {
    this.occupations = Set.copyOf(properties.getOccupations());
    this.educations = Set.copyOf(properties.getEducations());
    Map<String, IncomeBand> bands = new HashMap<>();
    for (IncomeBand band : properties.bands()) {
      bands.put(band.label(), band);
    }
    this.incomes = Map.copyOf(bands);
  }

  @Override
  public boolean occupation(String value) {
    return occupations.contains(value);
  }

  @Override
  public boolean education(String value) {
    return educations.contains(value);
  }

  @Override
  public boolean incomeBand(String label) {
    return incomes.containsKey(label);
  }

  @Override
  public Optional<IncomeBand> income(String label) {
    return Optional.ofNullable(incomes.get(label));
  }
}
