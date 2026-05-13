import com.google.gson.JsonObject
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.routing.Routing
import net.ccbluex.netty.http.routing.RoutingContext
import java.io.File

const val FOLDER_NAME = "files"
val folder = File(FOLDER_NAME)

suspend fun main() {
    val server = HttpServer()

    println("Serving files from: ${folder.absolutePath}")

    server.routing {
        fileRoutes()

        // register file serving at the bottom of the routing tree
        // to avoid overwriting other routes
        file("/", folder)
    }

    server.start(8080)  // Start the server on port 8080
}

fun Routing.fileRoutes() {
    get("/") { getRoot() }
    get("/conflicting") { getConflictingPath() }

    route("/a") {
        route("/b") {
            get("/c") { getConflictingPath() }
        }
    }

    route("/file") {
        get("/:name") { getFileInformation() }
        post("/:name") { postFile() }
    }
}

private suspend fun RoutingContext.getRoot() {
    respond(JsonObject().apply {
        addProperty("path", folder.absolutePath)
        addProperty("files", folder.walk().count())
    })
}

private suspend fun RoutingContext.getConflictingPath() {
    respond(JsonObject().apply {
        addProperty("message", "This is a conflicting path")
    })
}

private suspend fun RoutingContext.getFileInformation() {
    val name = parameters["name"] ?: badRequest("Missing name parameter")
    val file = File(folder, name)

    if (!file.exists()) {
        badRequest("File not found")
    }

    respond(JsonObject().apply {
        addProperty("name", file.name)
        addProperty("size", file.length())
        addProperty("lastModified", file.lastModified())
    })
}

private suspend fun RoutingContext.postFile() {
    val name = parameters["name"] ?: badRequest("Missing name parameter")
    val file = File(folder, name)

    if (file.exists()) {
        badRequest("File already exists")
    }

    file.writeText(body)
    respond(JsonObject().apply {
        addProperty("message", "File written")
    })
}
