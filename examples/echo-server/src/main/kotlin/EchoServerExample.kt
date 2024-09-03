import com.google.gson.JsonObject
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.util.httpOk

fun main() {
    val server = HttpServer()

    server.routeController.apply {
        post("/echo") { request ->
            httpOk(request.asJson<JsonObject>())
        }
    }

    server.start(8080)  // Start the server on port 8080
}
