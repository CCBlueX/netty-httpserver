import com.google.gson.JsonObject
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.routing.Routing
import net.ccbluex.netty.http.routing.RoutingContext

suspend fun main() {
    val server = HttpServer()

    server.routing {
        echoRoutes()
    }

    server.start(8080)  // Start the server on port 8080
}

fun Routing.echoRoutes() {
    route("/echo") {
        post { postEcho() }
    }
}

private suspend fun RoutingContext.postEcho() {
    respond(receive<JsonObject>())
}
