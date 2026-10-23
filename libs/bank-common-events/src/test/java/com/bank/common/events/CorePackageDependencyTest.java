package com.bank.common.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The core package stays free of Spring, JDBC and Kafka; adapters live in sub-packages. */
class CorePackageDependencyTest {

  @Test
  void coreImportsNoAdapterTechnology() throws IOException {
    Path core = Path.of("src/main/java/com/bank/common/events");
    List<Path> files;
    try (Stream<Path> s = Files.list(core)) {
      files = s.filter(p -> p.toString().endsWith(".java")).toList();
    }
    assertThat(files).isNotEmpty();
    for (Path file : files) {
      assertThat(Files.readString(file))
          .as(file.toString())
          .doesNotContain("import org.springframework")
          .doesNotContain("import java.sql")
          .doesNotContain("import org.apache.kafka");
    }
  }
}
