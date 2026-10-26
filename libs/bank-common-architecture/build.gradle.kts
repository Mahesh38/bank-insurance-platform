plugins {
    `java-library`
}

description = "EPIC-007 / ARCH-029: shared ArchUnit hexagonal baseline for every service module"

dependencies {
    api("com.tngtech.archunit:archunit-junit5:1.3.0")
    api("org.junit.jupiter:junit-jupiter")
    api("org.assertj:assertj-core")

    testImplementation("org.springframework:spring-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
