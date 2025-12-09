import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpOk
import java.io.File

const val FOLDER_NAME = "files"
val folder = File(FOLDER_NAME)

suspend fun main() {
    val server = HttpServer()

    println("Serving files from: ${folder.absolutePath}")

    server.routing {
        get("/", ::getRoot)
        get("/conflicting", ::getConflictingPath)
        get("/a/b/c", ::getConflictingPath)
        get("/file/:name", ::getFileInformation)
        post("/file/:name", ::postFile)

        // register file serving at the bottom of the routing tree
        // to avoid overwriting other routes
        file("/", folder)
    }

    server.start(8080)  // Start the server on port 8080
}

@Suppress("UNUSED_PARAMETER")
fun getRoot(requestObject: RequestObject): FullHttpResponse {
    // Count the number of files in the folder
    // Walk the folder and count the number of files
    return httpOk(JsonObject().apply {
        addProperty("path", folder.absolutePath)
        addProperty("files", folder.walk().count())
    })
}

@Suppress("UNUSED_PARAMETER")
fun getConflictingPath(requestObject: RequestObject): FullHttpResponse {
    return httpOk(JsonObject().apply {
        addProperty("message", "This is a conflicting path")
    })
}

@Suppress("UNUSED_PARAMETER")
fun getFileInformation(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.params["name"] ?: return httpBadRequest("Missing name parameter")
    val file = File(folder, name)

    if (!file.exists()) {
        return httpBadRequest("File not found")
    }

    return httpOk(JsonObject().apply {
        addProperty("name", file.name)
        addProperty("size", file.length())
        addProperty("lastModified", file.lastModified())
    })
}

@Suppress("UNUSED_PARAMETER")
fun postFile(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.params["name"] ?: return httpBadRequest("Missing name parameter")
    val file = File(folder, name)

    if (file.exists()) {
        return httpBadRequest("File already exists")
    }

    file.writeText(requestObject.body)
    return httpOk(JsonObject().apply {
        addProperty("message", "File written")
    })
}