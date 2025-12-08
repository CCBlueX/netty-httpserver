plugins {
    kotlin("jvm") version "2.0.0"
    application
}
group = "net.ccbluex"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(rootProject)
}

application {
    mainClass.set("FileServerExampleKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "FileServerExampleKt"
    }

    // Include runtime dependencies in the JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    // Prevent duplicate files from being added to the JAR
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}