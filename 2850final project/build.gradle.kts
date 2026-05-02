plugins {
    kotlin("jvm") version "1.9.22"
    id("application")
    id("com.gradleup.shadow") version "8.3.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

group = "com.goodfood"
version = "1.0.0"

// Static analysis — see ../detekt.yml for tuned rules.
detekt {
    toolVersion = "1.23.7"
    config.setFrom(rootProject.file("../detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
    parallel = true
    source.setFrom(files("src/main/kotlin", "src/test/kotlin"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(true)
        txt.required.set(false)
        md.required.set(false)
        xml.required.set(false)
    }
}

// Don't let Detekt findings fail `./gradlew build` — the CI workflow runs
// `./gradlew detekt` as a separate, non-blocking step. This keeps the build
// pipeline fast and lets the team tighten lint rules sprint-by-sprint without
// blocking PRs while the existing baseline is cleaned up.
tasks.named("check") {
    setDependsOn(dependsOn.filterNot {
        it is org.gradle.api.tasks.TaskProvider<*> && it.name.startsWith("detekt")
    })
}

application {
    mainClass.set("com.goodfood.ApplicationKt")
}

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"
val exposedVersion = "0.46.0"
val h2Version = "2.2.224"
val logbackVersion = "1.4.14"

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-thymeleaf-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktorVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("com.h2database:h2:$h2Version")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
}

kotlin {
    jvmToolchain(17)
}
