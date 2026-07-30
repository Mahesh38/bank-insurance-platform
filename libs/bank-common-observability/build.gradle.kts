plugins {
    `java-library`
}

description = "SHARED-004: Observability conventions - MDC keys, metric names, trace helpers"

dependencies {
    compileOnly("org.slf4j:slf4j-api")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("ch.qos.logback:logback-classic")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
