plugins {
    id("java")
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    implementation(project(":libs:bank-common-error"))
    implementation(project(":libs:bank-common-security"))
    implementation(project(":libs:bank-common-audit"))
    implementation(project(":libs:bank-common-observability"))
    // ADR-024: LEAD.EXCEPTION and LEAD.POLICY are evaluated by the shared engine.
    implementation(project(":libs:bank-rule-engine"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    runtimeOnly("org.postgresql:postgresql")
    // Flyway 10+ ships database support as modules: without it Flyway refuses PostgreSQL at start.
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

// PostgreSQL integration tests (PostgresMigrationIntegrationTest) run only with -Dpg.it=true and a
// reachable server (-Dpg.url, default the local socket-trust instance on port 55432).
tasks.test {
    systemProperty("pg.it", System.getProperty("pg.it") ?: "false")
    System.getProperty("pg.url")?.let { systemProperty("pg.url", it) }
}

tasks.bootJar {
    archiveFileName.set("lead-service.jar")
}
