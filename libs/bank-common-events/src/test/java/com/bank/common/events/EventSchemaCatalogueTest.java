package com.bank.common.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Every checked-in schema is a loadable 2020-12 schema and the catalogue lists each one. */
class EventSchemaCatalogueTest {

  @Test
  void everySchemaParsesAndIsListedInTheCatalogue() throws IOException {
    String catalogue = Files.readString(SchemaSupport.dir().resolveSibling("CATALOGUE.md"));
    List<Path> files;
    try (Stream<Path> s = Files.list(SchemaSupport.dir())) {
      files = s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
    }
    assertThat(files).hasSizeGreaterThan(30);
    for (Path file : files) {
      JsonNode node = SchemaSupport.MAPPER.readTree(file.toFile());
      assertThat(node.get("$schema").asText()).contains("2020-12");
      SchemaSupport.load(file.getFileName().toString());
      assertThat(catalogue)
          .as("CATALOGUE.md links " + file.getFileName())
          .contains("schemas/" + file.getFileName());
      if (!file.getFileName().toString().startsWith("event-envelope")) {
        assertThat(node.get("additionalProperties").asBoolean(true))
            .as("data schemas are closed so PII cannot ride along: " + file.getFileName())
            .isFalse();
      }
    }
  }

  @Test
  void dataSchemaRejectsUndeclaredFields() throws IOException {
    EventEnvelope event = TestEvents.paymentReconciled(Ulid.mint());
    ObjectNode data = SchemaSupport.MAPPER.valueToTree(event.data());
    data.put("customerName", "should never be here");

    assertThat(SchemaSupport.validate("PaymentReconciled.v1.json", data)).isNotEmpty();
  }

  @Test
  void paymentReconciledRequiresTheD8Ids() throws IOException {
    ObjectNode data =
        SchemaSupport.MAPPER.valueToTree(TestEvents.paymentReconciled(Ulid.mint()).data());
    for (String field : List.of("quoteId", "proposalId", "journeyId", "applicationNumber")) {
      ObjectNode copy = data.deepCopy();
      copy.remove(field);
      assertThat(SchemaSupport.validate("PaymentReconciled.v1.json", copy)).as(field).isNotEmpty();
    }
  }
}
