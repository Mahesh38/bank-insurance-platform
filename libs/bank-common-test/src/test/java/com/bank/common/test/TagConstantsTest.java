package com.bank.common.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.test.contract.ContractTags;
import com.bank.common.test.e2e.E2ETags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(PyramidTags.UNIT)
class TagConstantsTest {

  @Test
  void pyramidContractAndE2eTags_areStableStrings() {
    assertThat(PyramidTags.UNIT).isEqualTo("unit");
    assertThat(PyramidTags.INTEGRATION).isEqualTo("integration");
    assertThat(PyramidTags.TESTCONTAINERS).isEqualTo("testcontainers");
    assertThat(ContractTags.CONTRACT).isEqualTo("contract");
    assertThat(ContractTags.PERSISTENCE_JOBS_API).isEqualTo("contract:persistence-jobs");
    assertThat(E2ETags.E2E).isEqualTo("e2e");
    assertThat(E2ETags.ASSISTED_LIFE_SMOKE).isEqualTo("e2e:assisted-life-smoke");
  }
}
