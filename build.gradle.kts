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

    // Package-level floors from QA-LEAD-TESTING-STRATEGY §7, enforced since QA-001 closed.
    // Keyed by project path; each entry is (package glob, line floor, branch floor).
    // A glob that matches no package in the report would enforce nothing, so `packageFloorGuard`
    // below fails the build if one stops matching — otherwise deleting a package would silently
    // delete its gate.
    val packageFloors: Map<String, List<Triple<String, String, String>>> = mapOf(
        ":services:1sb-integration-service" to listOf(
            Triple("com.bank.insurance.onesb.application", "0.80", "0.70"),
            Triple("com.bank.insurance.onesb.lob", "0.80", "0.70"),
            Triple("com.bank.insurance.onesb.lob.life.term", "0.80", "0.70"),
            Triple("com.bank.insurance.onesb.adapter.onesb.*", "0.70", "0.60"),
            Triple("com.bank.insurance.onesb.adapter.idempotency", "0.80", "0.70"),
        ),
        ":services:bank-persistence-service" to listOf(
            Triple("com.bank.persistence.api.internal.v1", "0.70", "0.60"),
        ),
    )

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.named("jacocoTestReport"))
        classDirectories.setFrom(
            files(classDirectories.files.map { dir ->
                fileTree(dir) { exclude(coverageExcludes) }
            })
        )

        val isLib = project.path.startsWith(":libs:")
        // 1sb-integration-service raised to 90% line / 70% branch (measured ~91%/~71% —
        // see COVERAGE.md). WS-2 services (identity-*, workforce-access-bff) stay on the QA-002
        // interim 50% line floor: they are a different workstream at IAM Phase 1, and their
        // coverage is not what QA-001 / criterion 4.7 gates.
        val isOneSbIntegration = project.path == ":services:1sb-integration-service"
        val isPersistence = project.path == ":services:bank-persistence-service"
        val lineFloor = when {
            isLib -> "0.80"
            isOneSbIntegration -> "0.90"
            isPersistence -> "0.90"
            else -> "0.50"
        }.toBigDecimal()
        val branchFloor = when {
            isLib -> "0.70"
            isOneSbIntegration -> "0.70"
            isPersistence -> "0.70"
            else -> null
        }?.toBigDecimal()

        violationRules {
            // Module-wide floor.
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
            // Strategy §7 package floors — a module-wide average can hide a thin adapter.
            packageFloors[project.path].orEmpty().forEach { (pkg, line, branch) ->
                rule {
                    element = "PACKAGE"
                    includes = listOf(pkg)
                    limit {
                        counter = "LINE"
                        value = "COVEREDRATIO"
                        minimum = line.toBigDecimal()
                    }
                    limit {
                        counter = "BRANCH"
                        value = "COVEREDRATIO"
                        minimum = branch.toBigDecimal()
                    }
                }
            }
        }
    }

    // Guards the package rules above against silently matching nothing (renamed or removed
    // package, typo in a glob). Without this, a gate can disappear while the build stays green.
    val guarded = packageFloors[project.path].orEmpty()
    if (guarded.isNotEmpty()) {
        val guardTask = tasks.register("packageFloorGuard") {
            group = "verification"
            description = "Assert every strategy section 7 package floor still matches a real package."
            dependsOn(tasks.named("jacocoTestReport"))
            val reportFile = layout.buildDirectory
                .file("reports/jacoco/test/jacocoTestReport.xml")
            val globs = guarded.map { it.first }
            val projectPath = project.path
            inputs.file(reportFile)
            doLast {
                val xml = reportFile.get().asFile.readText()
                val present = Regex("""<package name="([^"]+)"""")
                    .findAll(xml)
                    .map { it.groupValues[1].replace('/', '.') }
                    .toList()
                val unmatched = globs.filter { glob ->
                    val regex = Regex(
                        glob.split("*").joinToString(".*") { Regex.escape(it) }
                    )
                    present.none { regex.matches(it) }
                }
                if (unmatched.isNotEmpty()) {
                    throw GradleException(
                        "$projectPath: package coverage floor(s) $unmatched match no package in the " +
                            "JaCoCo report, so they enforce nothing. Update the packageFloors table " +
                            "in build.gradle.kts and QA-LEAD-TESTING-STRATEGY section 7 together."
                    )
                }
            }
        }
        tasks.named("jacocoTestCoverageVerification") { dependsOn(guardTask) }
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
