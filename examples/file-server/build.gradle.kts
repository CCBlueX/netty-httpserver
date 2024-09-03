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
    implementation(project(":"))

    // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")
    // https://mvnrepository.com/artifact/io.netty/netty-all
    implementation("io.netty:netty-all:4.1.82.Final")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.10.1")
    // https://mvnrepository.com/artifact/org.apache.tika/tika-core
    implementation("org.apache.tika:tika-core:2.9.2")
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