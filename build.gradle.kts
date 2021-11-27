import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0"
}

group = "com.minhiew"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0")
    implementation("com.opencsv:opencsv:5.5.2")
    implementation("org.dom4j:dom4j:2.1.1")
    implementation("jaxen:jaxen:1.2.0")
    implementation("io.github.config4k:config4k:0.4.2")

    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.21.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest { attributes(mapOf("Main-Class" to "com.minhiew.translation.MainKt")) }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}