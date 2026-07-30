plugins {
    id("java")
    id("jacoco")
    id("org.springframework.boot") version "3.3.4" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

allprojects {
    group = "com.bank.insurance"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "io.spring.dependency-management")

    // Import the Spring Boot BOM for all subprojects (libs + service).
    // This lets lib modules declare compileOnly("org.slf4j:slf4j-api") etc.
    // without pinning versions — versions come from the BOM.
    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4")
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        // Lombok — version from Spring Boot BOM (TD-001)
        val lombok = "org.projectlombok:lombok"
        add("compileOnly", lombok)
        add("annotationProcessor", lombok)
        add("testCompileOnly", lombok)
        add("testAnnotationProcessor", lombok)
    }

    // QA-001 — JaCoCo coverage reports + verification (see docs/.../COVERAGE.md)
    configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    val coverageExcludes = listOf(
        "**/Application.class",
        "**/*Application.class",
        "**/package-info.class",
        "**/*Config.class",
        "**/*Configuration.class",
        "**/*Properties.class",
    )

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
        classDirectories.setFrom(
            files(classDirectories.files.map { dir ->
                fileTree(dir) { exclude(coverageExcludes) }
            })
        )
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.named("jacocoTestReport"))
        classDirectories.setFrom(
            files(classDirectories.files.map { dir ->
                fileTree(dir) { exclude(coverageExcludes) }
            })
        )

        val isLib = project.path.startsWith(":libs:")
        // Libs: strategy §7 (80% line / 70% branch).
        // Services: raised interim floor (QA-002) — package gates still pending QA-003.
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = if (isLib) "0.80".toBigDecimal() else "0.50".toBigDecimal()
                }
                if (isLib) {
                    limit {
                        counter = "BRANCH"
                        value = "COVEREDRATIO"
                        minimum = "0.70".toBigDecimal()
                    }
                }
            }
        }
    }

    // Make `check` (and typical CI `./gradlew test jacocoTestCoverageVerification`) enforce gates
    tasks.named("check") {
        dependsOn(tasks.named("jacocoTestCoverageVerification"))
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }
}
