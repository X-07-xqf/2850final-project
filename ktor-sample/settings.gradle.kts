rootProject.name = "ktor-sample"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
    }
}

include(":server")
include(":web")
