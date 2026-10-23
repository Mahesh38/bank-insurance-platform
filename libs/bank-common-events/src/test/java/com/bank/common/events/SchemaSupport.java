package com.bank.common.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** Loads the checked-in schemas from docs/platform/contracts/events/schemas. */
final class SchemaSupport {

  static final ObjectMapper MAPPER = new ObjectMapper();
  private static final JsonSchemaFactory FACTORY =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  private static final SchemaValidatorsConfig CONFIG =
      SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build();

  private SchemaSupport() {}

  static Path dir() {
    String dir =
        System.getProperty("events.schemaDir", "../../docs/platform/contracts/events/schemas");
    return Path.of(dir);
  }

  static JsonSchema load(String file) throws IOException {
    try (InputStream in = Files.newInputStream(dir().resolve(file))) {
      return FACTORY.getSchema(in, CONFIG);
    }
  }

  static Set<ValidationMessage> validate(String file, String json) throws IOException {
    JsonNode node = MAPPER.readTree(json);
    return load(file).validate(node);
  }

  static Set<ValidationMessage> validate(String file, JsonNode node) throws IOException {
    return load(file).validate(node);
  }
}
