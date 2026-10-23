plugins {
    `java-library`
}

description = "ADR-024: configuration-driven rule engine (rule-set/v1), its validator and resolver port"

dependencies {
    // The engine core parses rule-set documents with Jackson's tree model and nothing else from
    // the Spring stack (ADR-024 section 3: pure, deterministic, side-effect free).
    api("com.fasterxml.jackson.core:jackson-databind")

    // The HTTP resolver adapter (package com.bank.rules.client.http) is the only Spring-aware
    // code here. compileOnly, like bank-common-error: a deciding service already ships spring-web.
    compileOnly("org.springframework:spring-web")
    compileOnly("org.slf4j:slf4j-api")

    testImplementation("org.springframework:spring-web")
    testImplementation("org.slf4j:slf4j-api")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    // Test-only: proves the shipped JSON-Schema accepts every seed rule set. The runtime validator
    // is hand-written (RuleSetValidator) so the engine adds no schema library to service jars.
    testImplementation("com.networknt:json-schema-validator:1.5.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
