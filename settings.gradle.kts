plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "netty-httpserver"

include(":examples:hello-world")
include(":examples:echo-server")
include(":examples:file-server")
include(":examples:zip-server")