plugins {
    `java-library`
}

description = "SHARED-001: Bank error model — ServiceError, ServiceErrorResponse, ServiceException"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
