plugins {
    `java-library`
}

description = "SHARED-001: Bank error model — catalogue, envelope, diagnostic, shared handler"

dependencies {
    // Consumers supply Spring and a logging backend at runtime; the error model itself stays
    // usable from a plain Java module. Same pattern as bank-common-security / -secrets.
    compileOnly("org.springframework:spring-web")
    // MethodArgumentNotValidException extends validation.BindException, which lives here.
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("org.slf4j:slf4j-api")
    // MDC key names and the error counter. compileOnly, not implementation: every service already
    // declares bank-common-observability, and runtimeClasspath is dependency-locked, so a project
    // dependency here would churn lockfiles for classes the consumers already have.
    compileOnly(project(":libs:bank-common-observability"))
    compileOnly("io.micrometer:micrometer-core")

    testImplementation(project(":libs:bank-common-observability"))
    testImplementation("io.micrometer:micrometer-core")
    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-context")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.slf4j:slf4j-api")
    testImplementation("ch.qos.logback:logback-classic")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
