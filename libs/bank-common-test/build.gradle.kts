plugins {
    `java-library`
}

description = "S08-G6 / TD-014: shared Testcontainers, WireMock, fixtures, contract and E2E harness"

dependencies {
    // Testcontainers BOM — not in Spring Boot BOM
    api(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    api("org.testcontainers:testcontainers")
    api("org.testcontainers:junit-jupiter")
    api("org.testcontainers:postgresql")

    // WireMock 3 (same coordinate as 1sb-integration-service)
    api("org.wiremock:wiremock-standalone:3.9.1")

    api("org.junit.jupiter:junit-jupiter")
    api("org.assertj:assertj-core")

    // Optional Spring DynamicPropertyRegistry — present at test runtime in Boot services
    compileOnly("org.springframework:spring-test")
    compileOnly("org.springframework:spring-context")

    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-context")
    testRuntimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
