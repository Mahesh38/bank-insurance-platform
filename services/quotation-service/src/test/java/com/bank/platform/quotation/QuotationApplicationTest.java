package com.bank.platform.quotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.platform.quotation.application.QuoteMaterializer;
import com.bank.platform.quotation.support.EmbeddedDynamoDbTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(EmbeddedDynamoDbTestConfig.class)
class QuotationApplicationTest {

  @Autowired private QuoteMaterializer materializer;

  @Test
  void contextLoads() {
    assertThat(materializer).isNotNull();
  }
}
