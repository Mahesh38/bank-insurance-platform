plugins {
    `java-library`
}

description = "SHARED-003: Audit event model and publisher contract"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
