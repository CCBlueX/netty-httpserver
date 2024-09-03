import net.ccbluex.netty.http.HttpServer
import java.io.File

fun main() {
    val server = HttpServer()
    val folder = File("files")
    println("Serving files from: ${folder.absolutePath}")

    server.routeController.apply {
        // Serve files from the "files" directory
        file("/files", folder)
    }

    server.start(8080)  // Start the server on port 8080
}
