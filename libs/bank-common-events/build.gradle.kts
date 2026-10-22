plugins {
    `java-library`
}

description = "ADR-025: event envelope, transactional outbox relay, publisher port, idempotent consumers"

dependencies {
    // Core package (com.bank.common.events) is plain Java + Jackson. Every adapter dependency is
    // compileOnly so a service opts in by declaring it itself — same pattern as bank-common-error,
    // and it keeps this module's locked runtimeClasspath empty.
    compileOnly("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("org.slf4j:slf4j-api")
    // kafka adapter (com.bank.common.events.kafka): plain kafka-clients, not spring-kafka (ADR-025 §5).
    compileOnly("org.apache.kafka:kafka-clients")
    // spring adapter (com.bank.common.events.spring): join the caller's Spring-managed transaction.
    compileOnly("org.springframework:spring-jdbc")

    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.slf4j:slf4j-api")
    testImplementation("org.apache.kafka:kafka-clients")
    testImplementation("org.springframework:spring-jdbc")
    testImplementation("com.h2database:h2")
    // Validates envelope JSON against docs/platform/contracts/events/schemas (JSON-Schema 2020-12).
    testImplementation("com.networknt:json-schema-validator:1.5.6")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("ch.qos.logback:logback-classic")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// The envelope / event schemas are the contract the tests validate the codec against.
val eventSchemas = rootProject.file("docs/platform/contracts/events/schemas")
tasks.named<Test>("test") {
    inputs.dir(eventSchemas).withPropertyName("eventSchemas")
    systemProperty("events.schemaDir", eventSchemas.absolutePath)
}
