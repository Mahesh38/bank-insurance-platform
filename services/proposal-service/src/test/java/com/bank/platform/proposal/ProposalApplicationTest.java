package com.bank.platform.proposal;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.platform.proposal.application.ProposalMaterializer;
import com.bank.platform.proposal.support.EmbeddedDynamoDbTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(EmbeddedDynamoDbTestConfig.class)
class ProposalApplicationTest {

  @Autowired private ProposalMaterializer materializer;

  @Test
  void contextLoads() {
    assertThat(materializer).isNotNull();
  }
}
