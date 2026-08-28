plugins {
    `java-library`
}

description = "SHARED-004: Observability conventions - MDC keys, metric names, trace helpers"

dependencies {
    compileOnly("org.slf4j:slf4j-api")
    // Every service already ships spring-boot-starter-actuator, which brings micrometer-core, so
    // the registry is on the runtime classpath everywhere without adding a dependency (ADR-017
    // section 10). Consumers supply it, same pattern as slf4j above.
    compileOnly("io.micrometer:micrometer-core")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("ch.qos.logback:logback-classic")
    testImplementation("io.micrometer:micrometer-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
