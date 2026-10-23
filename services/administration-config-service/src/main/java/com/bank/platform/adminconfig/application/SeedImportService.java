package com.bank.platform.adminconfig.application;

import com.bank.platform.adminconfig.domain.RuleSetVersion;
import com.bank.platform.adminconfig.domain.RuleSetVersionRepository;
import com.bank.platform.adminconfig.domain.Ulid;
import com.bank.rules.engine.RuleSet;
import com.bank.rules.engine.RuleSetParser;
import com.bank.rules.engine.RuleSetSeeds;
import com.bank.rules.engine.Values;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports the engine library's seed rule sets as DRAFT versions when absent (ADR-024 section 7).
 * Never submits or publishes: content approval belongs to the named owner, and an agent or a
 * startup hook approving it would manufacture that approval.
 */
@Service
public class SeedImportService {

  public static final String SEED_MAKER = "system:seed-import";

  private static final Logger log = LoggerFactory.getLogger(SeedImportService.class);

  private final RuleSetVersionRepository repository;
  private final Clock clock;

  public SeedImportService(RuleSetVersionRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  /** Returns the ids (ruleSetId@version) imported by this call. */
  @Transactional
  public List<String> importAbsent(List<RuleSetSeeds.Seed> seeds) {
    List<String> imported = new ArrayList<>();
    for (RuleSetSeeds.Seed seed : seeds) {
      RuleSet parsed = RuleSetParser.parse(seed.json());
      if (repository.find(parsed.ruleSetId(), parsed.version()).isPresent()) {
        continue;
      }
      repository.insert(
          RuleSetVersion.draft(
              Ulid.mint(clock),
              parsed,
              Values.canonicalString(parsed.document()),
              SEED_MAKER,
              clock.instant()));
      imported.add(parsed.ruleSetId() + "@" + parsed.version());
    }
    if (!imported.isEmpty()) {
      log.info("Imported {} seed rule set(s) as DRAFT: {}", imported.size(), imported);
    }
    return imported;
  }
}
