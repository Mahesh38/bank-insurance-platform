plugins {
    `java-library`
}

description = "SHARED: Apigee client-credentials holder shared by customer-service and 1sb-integration-service"

dependencies {
    compileOnly("org.springframework:spring-web")
    compileOnly("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation("org.springframework:spring-web")
    testImplementation("org.springframework:spring-core")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.core:jackson-annotations")
    testImplementation("org.wiremock:wiremock-standalone:3.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
