package com.bank.platform.suitability.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SUIT-R40 (CR-010 Q-C3): the suitability hard gate must not be disableable by
 * feature flag, configuration, environment variable or request header in any
 * environment that holds customer data.
 *
 * <p>This is a structural negative. Introducing a bypass switch must fail the
 * build. The scan covers this module's {@code src/main} tree. A companion
 * ArchUnit rule in {@link ServiceArchitectureTest} refuses bypass-named types.
 */
class SuitR40NoBypassTest {

    private static final List<String> FORBIDDEN = List.of(
            "suitability.disabled",
            "suitability.enabled=false",
            "disable-suitability",
            "disable_suitability",
            "skip-suitability",
            "skip_suitability",
            "bypass-suitability",
            "bypass_suitability",
            "x-bypass-suitability",
            "x-skip-suitability",
            "suitabilitybypass",
            "bypasssuitabilitygate"
    );

    @Test
    void mainSourceContainsNoSuitabilityBypassSwitch() throws IOException {
        Path root = Path.of("src/main");
        assertThat(root).isDirectory();
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> hits = files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".java")
                                || name.endsWith(".yml")
                                || name.endsWith(".yaml")
                                || name.endsWith(".properties");
                    })
                    .filter(this::containsForbiddenToken)
                    .toList();
            assertThat(hits)
                    .as("SUIT-R40: no flag, config, env or header may disable the suitability gate")
                    .isEmpty();
        }
    }

    private boolean containsForbiddenToken(Path path) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            return FORBIDDEN.stream().anyMatch(text::contains);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to read " + path, ex);
        }
    }
}
