plugins {
    id("java")
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
}

dependencies {
    // Shared libs (stub modules until Dev A delivers final implementations)
    implementation(project(":libs:bank-common-error"))
    implementation(project(":libs:bank-common-security"))
    implementation(project(":libs:bank-common-audit"))
    implementation(project(":libs:bank-common-observability"))

    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Flyway DB migrations
    implementation("org.flywaydb:flyway-core")

    // Database drivers
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    // JSON
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
}

// Ensure the bootJar task has the correct name for a module path with special chars
tasks.bootJar {
    archiveFileName.set("1sb-integration-service.jar")
}
