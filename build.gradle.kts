plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
}

val projectName = "Netty-HttpServer"
val projectDescription = "A Kotlin library for web REST APIs built on top of Netty."
val licenseName = "GNU General Public License v3.0"
val licenseUrl = "https://www.gnu.org/licenses/gpl-3.0.en.html"
val authorName = "ccbluex"
val projectUrl = "https://github.com/ccbluex/netty-httpserver"

group = "net.ccbluex"
version = "2.3.2"

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
    // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
    api("org.apache.logging.log4j:log4j-core:2.23.1")
    // https://mvnrepository.com/artifact/io.netty/netty-all
    api("io.netty:netty-all:4.1.115.Final")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    api("com.google.code.gson:gson:2.10.1")
    // https://mvnrepository.com/artifact/org.apache.tika/tika-core
    api("org.apache.tika:tika-core:2.9.2")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.retrofit2:retrofit:2.9.0")
    testImplementation("com.squareup.retrofit2:converter-gson:2.9.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.release.set(21)
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to projectName,
            "Implementation-Version" to version,
            "Implementation-Vendor" to authorName,
            "License" to licenseName,
            "License-Url" to licenseUrl
        )
    }

    // Include LICENSE file in the JAR
    from("LICENSE") {
        into("META-INF/")
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

publishing {
    publications {
        create<MavenPublication>("pub") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            pom {
                name.set(projectName)
                description.set(projectDescription)
                url.set(projectUrl)

                licenses {
                    license {
                        name.set(licenseName)
                        url.set(licenseUrl)
                    }
                }

                developers {
                    developer {
                        id.set("ccbluex")
                        name.set(authorName)
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/ccbluex/netty-httpserver.git")
                    developerConnection.set("scm:git:ssh://github.com:ccbluex/netty-httpserver.git")
                    url.set(projectUrl)
                }
            }
        }
    }

    repositories {
        maven {
            name = "ccbluex-maven"
            url = uri("https://maven.ccbluex.net/releases")
            credentials {
                username = System.getenv("MAVEN_TOKEN_NAME")
                password = System.getenv("MAVEN_TOKEN_SECRET")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
