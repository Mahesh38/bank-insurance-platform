package com.bank.rules.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The R0 seed rule sets shipped in this library under {@code rulesets/} (ADR-024 section 7). They
 * live beside the engine so the engine's own build proves every seed parses and passes its test
 * cases; the configuration service imports them as DRAFT versions and never publishes them itself.
 */
public final class RuleSetSeeds {

  static final String INDEX = "rulesets/index.txt";

  private RuleSetSeeds() {}

  /** One seed: its classpath resource name and raw JSON text. */
  public record Seed(String resource, String json) {}

  public static List<Seed> all() {
    ClassLoader cl = RuleSetSeeds.class.getClassLoader();
    List<Seed> out = new ArrayList<>();
    for (String name : read(cl, INDEX).split("\\R")) {
      String trimmed = name.strip();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
        continue;
      }
      String resource = "rulesets/" + trimmed;
      out.add(new Seed(resource, read(cl, resource)));
    }
    return List.copyOf(out);
  }

  private static String read(ClassLoader cl, String resource) {
    try (InputStream in = cl.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("missing seed resource " + resource);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
