plugins {
    id("java")
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    // Shared libs
    implementation(project(":libs:bank-common-error"))
    implementation(project(":libs:bank-common-security"))
    implementation(project(":libs:bank-common-audit"))
    implementation(project(":libs:bank-common-observability"))
    implementation(project(":libs:bank-common-secrets"))

    // Spring Boot starters (no JPA / Flyway — persistence is a separate service)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // JSON
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // OpenAPI / Swagger UI for the bank-facing API
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("org.wiremock:wiremock-standalone:3.9.1")
}

tasks.bootJar {
    archiveFileName.set("1sb-integration-service.jar")
}

// Forward -DupdateOpenApi=true from the Gradle JVM to the test JVM so OpenApiContractTest can
// regenerate the published spec. Without this the flag is silently ignored and the test just
// reports drift, which looks like the regeneration failed.
tasks.withType<Test> {
    System.getProperty("updateOpenApi")?.let { systemProperty("updateOpenApi", it) }
    System.getProperty("updateAuditSamples")?.let { systemProperty("updateAuditSamples", it) }
    // QuoteLatencySmokeIT opt-in switch and its tunables (criterion 4.6).
    listOf(
        "perf.enabled", "perf.concurrency", "perf.requests",
        "perf.warmup", "perf.upstreamDelayMs", "perf.overheadBudgetMs", "perf.minConcurrencyGain",
    ).forEach { key -> System.getProperty(key)?.let { systemProperty(key, it) } }
}
