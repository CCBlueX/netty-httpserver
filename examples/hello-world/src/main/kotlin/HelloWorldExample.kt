import com.google.gson.JsonObject
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.util.httpOk

fun main() {
    val server = HttpServer()

    server.routeController.apply {
        get("/hello") {
            httpOk(JsonObject().apply {
                addProperty("message", "Hello, World!")
            })
        }
    }

    server.start(8080)  // Start the server on port 8080
}
