plugins {
    id("java")
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    implementation(project(":libs:bank-common-error"))
    implementation(project(":libs:bank-common-observability"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.flywaydb:flyway-core")
    // Flyway 10+ ships database support as separate modules; required for Postgres ITs (S08-G6).
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // OpenAPI / Swagger UI for the internal API
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // S08-G6 / TD-014 — shared Testcontainers harness
    testImplementation(project(":libs:bank-common-test"))
}

tasks.bootJar {
    archiveFileName.set("bank-persistence-service.jar")
}
