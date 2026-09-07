plugins {
    `java-library`
}

description = "Bank-owned domain models shared across services (not 1SB wire types)"

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
