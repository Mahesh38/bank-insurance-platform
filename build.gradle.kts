plugins {
    java
}

allprojects {
    group = "com.bank.insurance"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        // Make Spring Boot BOM available for version resolution in every subproject.
        // Individual libs add only what they need via compileOnly/implementation.
        add("implementation", platform("org.springframework.boot:spring-boot-dependencies:3.3.5"))

        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.assertj:assertj-core")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
