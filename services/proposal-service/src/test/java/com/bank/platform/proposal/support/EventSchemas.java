package com.bank.platform.proposal.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** The checked-in event contract: docs/platform/contracts/events/schemas (JSON-Schema 2020-12). */
public final class EventSchemas {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final JsonSchemaFactory FACTORY =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  private static final SchemaValidatorsConfig CONFIG =
      SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build();

  private EventSchemas() {}

  public static Path dir() {
    return Path.of(
        System.getProperty("events.schemaDir", "../../docs/platform/contracts/events/schemas"));
  }

  public static boolean exists(String eventType) {
    return Files.exists(dir().resolve(eventType + ".v1.json"));
  }

  /** Envelope against event-envelope.v1.json, its data against {@code <eventType>.v1.json}. */
  public static Set<ValidationMessage> validateEnvelope(String json) {
    try {
      JsonNode node = MAPPER.readTree(json);
      Set<ValidationMessage> problems = validate("event-envelope.v1.json", node);
      String type = node.get("eventType").asText();
      problems.addAll(
          validate(type + ".v" + node.get("schemaVersion").asInt() + ".json", node.get("data")));
      return problems;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Set<ValidationMessage> validate(String file, JsonNode node) throws IOException {
    try (InputStream in = Files.newInputStream(dir().resolve(file))) {
      return new java.util.LinkedHashSet<>(FACTORY.getSchema(in, CONFIG).validate(node));
    }
  }
}
