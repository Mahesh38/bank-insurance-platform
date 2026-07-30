plugins {
    java
}

description = "SHARED-002: JWT principal model and security configuration properties"

dependencies {
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.security:spring-security-core")
    compileOnly("org.springframework.security:spring-security-web")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
