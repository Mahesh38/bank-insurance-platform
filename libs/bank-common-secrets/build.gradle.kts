plugins {
    `java-library`
}

description = "Shared secrets SPI — SecretProvider and PROPERTIES/ENV/AWS providers"

dependencies {
    // Spring Environment used by PropertiesSecretProvider; consumers supply Spring at runtime
    compileOnly("org.springframework:spring-core")

    testImplementation("org.springframework:spring-core")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
