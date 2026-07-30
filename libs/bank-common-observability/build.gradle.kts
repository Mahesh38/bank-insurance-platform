plugins {
    java
}

description = "SHARED-004: Observability conventions - MDC keys, metric names, trace helpers"

dependencies {
    compileOnly("org.slf4j:slf4j-api")
    // logback-classic provides MDC support needed for MdcContextTest
    testImplementation("ch.qos.logback:logback-classic")
}
