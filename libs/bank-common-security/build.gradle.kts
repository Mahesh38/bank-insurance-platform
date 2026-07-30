plugins {
    `java-library`
}

description = "SHARED-002: JWT principal model and security configuration properties"

dependencies {
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.security:spring-security-core")
    compileOnly("org.springframework.security:spring-security-web")
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
