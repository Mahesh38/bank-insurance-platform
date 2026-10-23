package com.bank.platform.adminconfig.adapter.seed;

import com.bank.platform.adminconfig.application.SeedImportService;
import com.bank.rules.engine.RuleSetSeeds;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Imports the engine's seed rule sets as DRAFT at startup (ADR-024 section 7). */
@Component
@ConditionalOnProperty(
    name = "admin-config.seed-import.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SeedImportRunner implements ApplicationRunner {

  private final SeedImportService seeds;

  public SeedImportRunner(SeedImportService seeds) {
    this.seeds = seeds;
  }

  @Override
  public void run(ApplicationArguments args) {
    seeds.importAbsent(RuleSetSeeds.all());
  }
}
