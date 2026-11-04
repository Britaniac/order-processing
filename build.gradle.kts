plugins {
    base
    id("org.springframework.boot") version "4.1.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.orderflow"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    repositories { mavenCentral() }
    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion = JavaLanguageVersion.of(25) }
    }
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}

tasks.named("build") { dependsOn(subprojects.map { it.tasks.named("build") }) }
tasks.named("check") { dependsOn(subprojects.map { it.tasks.named("check") }) }
tasks.named("clean") { dependsOn(subprojects.map { it.tasks.named("clean") }) }
