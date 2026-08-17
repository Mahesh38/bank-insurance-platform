plugins {
    id("java")
    id("jacoco")
    id("org.springframework.boot") version "3.5.16" apply false
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
    // Spring Boot 3.5.16 pins Netty 4.1.135.Final, which is still exposed to
    // CVE-2026-59901 (fixed in 4.1.136.Final). Overriding the BOM property is a
    // single patch bump inside the same minor line, and it is the only override
    // here — every other flagged package is fixed by the BOM itself.
    // Remove this once a Spring Boot release pins 4.1.136.Final or later.
    extra["netty.version"] = "4.1.136.Final"

    // Same pattern: 3.5.16 pins PostgreSQL 42.7.11, exposed to CVE-2026-54291
    // (fixed in 42.7.12). Remove once a Spring Boot release pins 42.7.12 or later.
    extra["postgresql.version"] = "42.7.12"

    configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.16")
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    repositories {
        mavenCentral()
    }

    // S08-E04-S03/S05 — dependency locking, so the SCA and SBOM tooling can read
    // the resolved Java dependency graph from a file it parses natively.
    //
    // Trivy could not see the Java estate otherwise: its filesystem scan found only
    // the Flutter pubspec.lock, and staging the Spring Boot fat jars into the scan
    // root did not help, because Trivy's JAR analyzer needs trivy-java-db to
    // identify archives and that lookup was not resolving. A gradle.lockfile is a
    // plain manifest Trivy parses directly, with no external database involved.
    //
    // Only runtimeClasspath is locked. That is the configuration that describes what
    // actually ships, which is exactly what an SBOM and a CVE scan should cover, and
    // it keeps compile- and test-only churn out of the lockfiles.
    dependencyLocking {
        lockMode.set(LockMode.LENIENT)
    }
    configurations.matching { it.name == "runtimeClasspath" }.configureEach {
        resolutionStrategy.activateDependencyLocking()
    }

    // Writes every lockfile in one invocation: ./gradlew resolveAndLockAll --write-locks
    tasks.register("resolveAndLockAll") {
        notCompatibleWithConfigurationCache("Resolves configurations at execution time")
        doFirst {
            require(gradle.startParameter.isWriteDependencyLocks) {
                "Run with --write-locks"
            }
        }
        doLast {
            configurations
                .matching { it.name == "runtimeClasspath" }
                .forEach { it.resolve() }
        }
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
        // 1sb-integration-service raised to 90% line / 70% branch (measured ~90.7%/~71% —
        // see COVERAGE.md). Other services keep the QA-002 interim 50% line floor pending QA-003
        // package-level gates.
        val isOneSbIntegration = project.path == ":services:1sb-integration-service"
        val lineFloor = when {
            isLib -> "0.80"
            isOneSbIntegration -> "0.90"
            else -> "0.50"
        }.toBigDecimal()
        val branchFloor = when {
            isLib -> "0.70"
            isOneSbIntegration -> "0.70"
            else -> null
        }?.toBigDecimal()
        // Libs: strategy §7 (80% line / 70% branch).
        // Services: raised interim floor (QA-002) — package gates still pending QA-003.
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = lineFloor
                }
                if (branchFloor != null) {
                    limit {
                        counter = "BRANCH"
                        value = "COVEREDRATIO"
                        minimum = branchFloor
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

// ---------------------------------------------------------------------------
// Governance (AIGEM) — docs/governance/RUNBOOK.md
//
// The freshness check is a single-file Java 21 program so it runs on the
// documented baseline (JDK + Git) with no build step and no dependencies.
// This task is a convenience wrapper; `java scripts/governance/FreshnessCheck.java`
// works identically and is what agents invoke.
// ---------------------------------------------------------------------------
tasks.register<Exec>("governanceFreshness") {
    group = "verification"
    description = "Check that the AIGEM governance state is fresh enough for agents to trust."
    workingDir = rootDir
    val java = "${System.getProperty("java.home")}/bin/java"
    commandLine(java, "scripts/governance/FreshnessCheck.java")
    isIgnoreExitValue = true
    doLast {
        val code = executionResult.get().exitValue
        // 1 = warnings: reported, not fatal. 2 = halt-class staleness: fail the build.
        if (code >= 2) {
            throw GradleException(
                "Governance state is stale (exit $code). Agents must not admit new work; " +
                "see docs/governance/RUNBOOK.md section 4."
            )
        }
    }
}
