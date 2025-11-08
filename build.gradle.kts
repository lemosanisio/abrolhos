plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    id("org.springframework.boot") version "3.4.8"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.23"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.openapi.generator") version "7.8.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "br.dev.demoraes"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set(project.file("src/main/resources/openapi.yml").absolutePath)
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    apiPackage.set("br.dev.demoraes.abrolhos.Application.api")
    modelPackage.set("br.dev.demoraes.abrolhos.Application.dto")
    invokerPackage.set("br.dev.demoraes.abrolhos.Application.invoker")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useTags" to "true",
            "dateLibrary" to "java21",
            "serializationLibrary" to "jackson",
            "reactive" to "false",
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
        ),
    )
}

sourceSets {
    main {
        java.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin"))
    }
}

tasks.named("compileKotlin") {
    dependsOn("openApiGenerate")
}

dependencies {
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.aallam.ulid:ulid-kotlin:1.4.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-hateoas")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.github.wimdeblauwe:htmx-spring-boot-thymeleaf:4.0.1")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    toolVersion = "1.23.6"
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    val baselineFile = file("$rootDir/config/detekt/baseline.xml")
    if (baselineFile.exists()) {
        baseline = baselineFile
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"
    reports {
        html.required.set(true)
        xml.required.set(false)
        sarif.required.set(false)
    }
}

tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektFormat") {
    description = "Runs Detekt with auto-correct to format Kotlin sources using the detekt-formatting ruleset."
    group = "verification"

    // Sources to check/format
    setSource(files("src"))
    include("**/*.kt", "**/*.kts")
    exclude("**/build/**", "**/generated/**")

    // Keep the same config/baseline as the main detekt task
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    val formatBaselineFile = file("$rootDir/config/detekt/baseline.xml")
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

ktlint {
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
    }
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
        include("src/**/*.kt")
        include("**/*.kts")
    }
}

// Ensure ktlint runs with the standard 'check' lifecycle
tasks.named("check") {
    dependsOn("ktlintCheck")
}
