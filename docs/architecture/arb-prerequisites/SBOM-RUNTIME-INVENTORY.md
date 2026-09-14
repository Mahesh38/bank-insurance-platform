# Software dependency inventory (runtimeClasspath lockfiles)

**Generated:** 2026-09-14 from `gradle.lockfile` files (S08-E04-S03). This is the inventory ARB can read today. A CycloneDX/SPDX export in CI is the S09 hardening of the same graph, not a second estate.

- Modules with lockfiles: **14**
- Unique `group:name:version` on `runtimeClasspath`: **137**
- Language / runtime: **Java 21**, **Spring Boot 3.5.16** BOM, Netty override `4.1.137.Final`, PostgreSQL JDBC `42.7.12`, Tomcat embed `10.1.59` (root `build.gradle.kts`).
- Flutter NIP-APP `pubspec.lock` is a separate client inventory and is not merged here.

## Modules locked

- `libs`
- `libs/bank-common-audit`
- `libs/bank-common-domain`
- `libs/bank-common-error`
- `libs/bank-common-observability`
- `libs/bank-common-secrets`
- `libs/bank-common-security`
- `libs/bank-common-test`
- `services`
- `services/1sb-integration-service`
- `services/bank-persistence-service`
- `services/identity-authorization-service`
- `services/identity-provider-adapter-service`
- `services/workforce-access-bff`

## Runtime coordinates

| Coordinate | Locked in |
|---|---|
| `ch.qos.logback:logback-classic:1.5.34` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `ch.qos.logback:logback-core:1.5.34` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `com.fasterxml.jackson.core:jackson-annotations:2.21` | `libs/bank-common-test`, `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `com.fasterxml.jackson.core:jackson-core:2.21.4` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `com.fasterxml.jackson.core:jackson-databind:2.21.4` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.21.4` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.21.4` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.21.4` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.4` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `com.fasterxml.jackson.module:jackson-module-parameter-names:2.21.4` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `com.fasterxml.jackson:jackson-bom:2.21.4` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `com.fasterxml:classmate:1.7.3` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `com.github.docker-java:docker-java-api:3.4.2` | `libs/bank-common-test` |
| `com.github.docker-java:docker-java-transport-zerodep:3.4.2` | `libs/bank-common-test` |
| `com.github.docker-java:docker-java-transport:3.4.2` | `libs/bank-common-test` |
| `com.github.stephenc.jcip:jcip-annotations:1.0-1` | `services/identity-provider-adapter-service` |
| `com.h2database:h2:2.3.232` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `com.nimbusds:nimbus-jose-jwt:9.37.4` | `services/identity-provider-adapter-service` |
| `com.sun.istack:istack-commons-runtime:4.1.2` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `com.zaxxer:HikariCP:6.3.3` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `io.lettuce:lettuce-core:6.6.0.RELEASE` | `services/workforce-access-bff` |
| `io.micrometer:micrometer-commons:1.15.12` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `io.micrometer:micrometer-core:1.15.12` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `io.micrometer:micrometer-jakarta9:1.15.12` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `io.micrometer:micrometer-observation:1.15.12` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `io.netty:netty-buffer:4.1.137.Final` | `services/workforce-access-bff` |
| `io.netty:netty-codec:4.1.137.Final` | `services/workforce-access-bff` |
| `io.netty:netty-common:4.1.137.Final` | `services/workforce-access-bff` |
| `io.netty:netty-handler:4.1.137.Final` | `services/workforce-access-bff` |
| `io.netty:netty-resolver:4.1.137.Final` | `services/workforce-access-bff` |
| `io.netty:netty-transport-native-unix-common:4.1.137.Final` | `services/workforce-access-bff` |
| `io.netty:netty-transport:4.1.137.Final` | `services/workforce-access-bff` |
| `io.projectreactor:reactor-core:3.7.19` | `services/workforce-access-bff` |
| `io.smallrye:jandex:3.2.0` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `io.swagger.core.v3:swagger-annotations-jakarta:2.2.22` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `io.swagger.core.v3:swagger-core-jakarta:2.2.22` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `io.swagger.core.v3:swagger-models-jakarta:2.2.22` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `jakarta.activation:jakarta.activation-api:2.1.4` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `jakarta.annotation:jakarta.annotation-api:2.1.1` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `jakarta.inject:jakarta.inject-api:2.0.1` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `jakarta.persistence:jakarta.persistence-api:3.1.0` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `jakarta.transaction:jakarta.transaction-api:2.0.1` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `jakarta.validation:jakarta.validation-api:3.0.2` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `jakarta.xml.bind:jakarta.xml.bind-api:4.0.5` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `junit:junit:4.13.2` | `libs/bank-common-test` |
| `net.bytebuddy:byte-buddy:1.17.8` | `libs/bank-common-test`, `services/bank-persistence-service`, `services/identity-authorization-service` |
| `net.java.dev.jna:jna:5.13.0` | `libs/bank-common-test` |
| `org.antlr:antlr4-runtime:4.13.2` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.apache.commons:commons-compress:1.24.0` | `libs/bank-common-test` |
| `org.apache.commons:commons-lang3:3.17.0` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.apache.logging.log4j:log4j-api:2.24.3` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.apache.logging.log4j:log4j-to-slf4j:2.24.3` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.apache.tomcat.embed:tomcat-embed-core:10.1.59` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.apache.tomcat.embed:tomcat-embed-el:10.1.59` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.apache.tomcat.embed:tomcat-embed-websocket:10.1.59` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.aspectj:aspectjweaver:1.9.25.1` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.assertj:assertj-core:3.27.7` | `libs/bank-common-test` |
| `org.eclipse.angus:angus-activation:2.0.3` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.flywaydb:flyway-core:11.7.2` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.flywaydb:flyway-database-postgresql:11.7.2` | `services/bank-persistence-service` |
| `org.glassfish.jaxb:jaxb-core:4.0.9` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.glassfish.jaxb:jaxb-runtime:4.0.9` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.glassfish.jaxb:txw2:4.0.9` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.hamcrest:hamcrest-core:3.0` | `libs/bank-common-test` |
| `org.hamcrest:hamcrest:3.0` | `libs/bank-common-test` |
| `org.hdrhistogram:HdrHistogram:2.2.2` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.hibernate.common:hibernate-commons-annotations:7.0.3.Final` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.hibernate.orm:hibernate-core:6.6.53.Final` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.hibernate.validator:hibernate-validator:8.0.3.Final` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.jboss.logging:jboss-logging:3.6.3.Final` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.jetbrains:annotations:17.0.0` | `libs/bank-common-test` |
| `org.junit.jupiter:junit-jupiter-api:5.12.2` | `libs/bank-common-test` |
| `org.junit.jupiter:junit-jupiter-engine:5.12.2` | `libs/bank-common-test` |
| `org.junit.jupiter:junit-jupiter-params:5.12.2` | `libs/bank-common-test` |
| `org.junit.jupiter:junit-jupiter:5.12.2` | `libs/bank-common-test` |
| `org.junit.platform:junit-platform-commons:1.12.2` | `libs/bank-common-test` |
| `org.junit.platform:junit-platform-engine:1.12.2` | `libs/bank-common-test` |
| `org.junit:junit-bom:5.12.2` | `libs/bank-common-test` |
| `org.latencyutils:LatencyUtils:2.0.3` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.opentest4j:opentest4j:1.3.0` | `libs/bank-common-test` |
| `org.postgresql:postgresql:42.7.12` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.reactivestreams:reactive-streams:1.0.4` | `services/workforce-access-bff` |
| `org.rnorth.duct-tape:duct-tape:1.0.8` | `libs/bank-common-test` |
| `org.slf4j:jul-to-slf4j:2.0.18` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.slf4j:slf4j-api:2.0.18` | `libs/bank-common-test`, `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springdoc:springdoc-openapi-starter-common:2.6.0` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springdoc:springdoc-openapi-starter-webmvc-api:2.6.0` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-actuator-autoconfigure:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-actuator:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-autoconfigure:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-starter-actuator:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-starter-data-jpa:3.5.16` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.springframework.boot:spring-boot-starter-data-redis:3.5.16` | `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-starter-jdbc:3.5.16` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.springframework.boot:spring-boot-starter-json:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-starter-logging:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-starter-security:3.5.16` | `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-starter-tomcat:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-starter-validation:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-starter-web:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot-starter:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.boot:spring-boot:3.5.16` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.data:spring-data-commons:3.5.13` | `services/bank-persistence-service`, `services/identity-authorization-service`, `services/workforce-access-bff` |
| `org.springframework.data:spring-data-jpa:3.5.13` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.springframework.data:spring-data-keyvalue:3.5.13` | `services/workforce-access-bff` |
| `org.springframework.data:spring-data-redis:3.5.13` | `services/workforce-access-bff` |
| `org.springframework.security:spring-security-config:6.5.11` | `services/workforce-access-bff` |
| `org.springframework.security:spring-security-core:6.5.11` | `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.security:spring-security-crypto:6.5.11` | `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework.security:spring-security-oauth2-core:6.5.11` | `services/identity-provider-adapter-service` |
| `org.springframework.security:spring-security-oauth2-jose:6.5.11` | `services/identity-provider-adapter-service` |
| `org.springframework.security:spring-security-web:6.5.11` | `services/workforce-access-bff` |
| `org.springframework:spring-aop:6.2.19` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework:spring-aspects:6.2.19` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.springframework:spring-beans:6.2.19` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework:spring-context-support:6.2.19` | `services/workforce-access-bff` |
| `org.springframework:spring-context:6.2.19` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework:spring-core:6.2.19` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework:spring-expression:6.2.19` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework:spring-jcl:6.2.19` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework:spring-jdbc:6.2.19` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.springframework:spring-orm:6.2.19` | `services/bank-persistence-service`, `services/identity-authorization-service` |
| `org.springframework:spring-oxm:6.2.19` | `services/workforce-access-bff` |
| `org.springframework:spring-tx:6.2.19` | `services/bank-persistence-service`, `services/identity-authorization-service`, `services/workforce-access-bff` |
| `org.springframework:spring-web:6.2.19` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.springframework:spring-webmvc:6.2.19` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.testcontainers:database-commons:1.21.4` | `libs/bank-common-test` |
| `org.testcontainers:jdbc:1.21.4` | `libs/bank-common-test` |
| `org.testcontainers:junit-jupiter:1.21.4` | `libs/bank-common-test` |
| `org.testcontainers:postgresql:1.21.4` | `libs/bank-common-test` |
| `org.testcontainers:testcontainers-bom:1.20.4` | `libs/bank-common-test` |
| `org.testcontainers:testcontainers:1.21.4` | `libs/bank-common-test` |
| `org.webjars:swagger-ui:5.17.14` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `org.wiremock:wiremock-standalone:3.9.1` | `libs/bank-common-test` |
| `org.yaml:snakeyaml:2.4` | `services/1sb-integration-service`, `services/bank-persistence-service`, `services/identity-authorization-service`, `services/identity-provider-adapter-service`, `services/workforce-access-bff` |
| `redis.clients.authentication:redis-authx-core:0.1.1-beta2` | `services/workforce-access-bff` |
