plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    id("org.springframework.boot") version "3.4.8"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.23"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.openapi.generator") version "7.8.0"
}

group = "br.dev.demoraes"

version = "0.0.1-SNAPSHOT"

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories { mavenCentral() }

dependencies {
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.aallam.ulid:ulid-kotlin:1.4.0")
    implementation("dev.turingcomplete:kotlin-onetimepassword:2.4.1")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> { useJUnitPlatform() }

detekt {
    toolVersion = "1.23.6"
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/src/main/resources/detekt.yml"))
    val baselineFile = file("$rootDir/src/main/resources/detekt/baseline.xml")
    if (baselineFile.exists()) {
        baseline = baselineFile
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/generated/**")
    jvmTarget = "21"
    reports {
        html.required.set(true)
        xml.required.set(false)
        sarif.required.set(false)
    }
}

tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektFormat") {
    description =
            "Runs Detekt with auto-correct to format Kotlin sources using the detekt-formatting ruleset."
    group = "verification"

    // Sources to check/format
    setSource(files("src"))
    include("**/*.kt", "**/*.kts")
    exclude("**/build/**", "**/generated/**")

    // Keep the same config/baseline as the main detekt task
    config.setFrom(files("$rootDir/src/main/resources/detekt.yml"))
    val formatBaselineFile = file("$rootDir/src/main/resources/detekt/baseline.xml")
    if (formatBaselineFile.exists()) {
        baseline.set(formatBaselineFile)
    }
    buildUponDefaultConfig = true

    // Enable formatting fixes
    autoCorrect = true
    jvmTarget = "21"

    reports {
        html.required.set(true)
        xml.required.set(false)
        sarif.required.set(false)
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    val envFile = file("local.env")
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            if (line.isNotBlank() && !line.startsWith("#")) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    environment(parts[0], parts[1])
                }
            }
        }
    }
}
