plugins {
    id("java")
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.6"
}

// DB-DEC-0005: working proposal in DynamoDB, structured rows in Aurora after RECONCILED.
val awsSdkVersion = "2.55.16"
val dynamoDbLocalVersion = "2.6.1"
val sqlite4javaVersion = "1.0.392"

// sqlite4java native libraries for in-process DynamoDB Local (tests only, no Docker).
val sqlite4javaNative: Configuration by configurations.creating

dependencies {
    implementation(project(":libs:bank-common-error"))
    implementation(project(":libs:bank-common-audit"))
    implementation(project(":libs:bank-common-observability"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // AWS SDK v2 with the JDK URL-connection client only; Netty and Apache clients excluded.
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))
    for (module in listOf("dynamodb", "kms")) {
        implementation("software.amazon.awssdk:$module") {
            exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
            exclude(group = "software.amazon.awssdk", module = "apache-client")
            exclude(group = "software.amazon.awssdk", module = "apache5-client")
        }
    }
    implementation("software.amazon.awssdk:url-connection-client")

    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testImplementation("com.amazonaws:DynamoDBLocal:$dynamoDbLocalVersion")
    sqlite4javaNative("com.almworks.sqlite4java:libsqlite4java-linux-amd64:$sqlite4javaVersion@so")
    sqlite4javaNative("com.almworks.sqlite4java:libsqlite4java-osx:$sqlite4javaVersion@dylib")
}

val sqlite4javaDir = layout.buildDirectory.dir("sqlite4java")

val copySqlite4javaNatives by tasks.registering(Copy::class) {
    from(sqlite4javaNative)
    into(sqlite4javaDir)
    rename { it.replace("-$sqlite4javaVersion", "") }
}

// DynamoDB Local writes dynamodb-local-metadata.json into the working directory; keep it in build/.
val dynamoDbLocalWorkDir = layout.buildDirectory.dir("dynamodb-local")

tasks.test {
    dependsOn(copySqlite4javaNatives)
    doFirst { dynamoDbLocalWorkDir.get().asFile.mkdirs() }
    workingDir = dynamoDbLocalWorkDir.get().asFile
    systemProperty("sqlite4java.library.path", sqlite4javaDir.get().asFile.absolutePath)
}

tasks.bootJar {
    archiveFileName.set("proposal-service.jar")
}
