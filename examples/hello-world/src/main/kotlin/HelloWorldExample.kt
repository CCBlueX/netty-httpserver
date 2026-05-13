import com.google.gson.JsonObject
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.routing.Routing
import net.ccbluex.netty.http.routing.RoutingContext

suspend fun main() {
    val server = HttpServer()

    server.routing {
        helloWorldRoutes()
    }

    server.start(8080)  // Start the server on port 8080
}

fun Routing.helloWorldRoutes() {
    get("/") { getRoot() }

    route("/hello") {
        get { getHello() } // /hello?name=World
        get("/:name") { getHello() } // /hello/World
        get("/:name/:age") { getHelloWithAge() } // /hello/World/20
    }
}

private suspend fun RoutingContext.getRoot() {
    respond(JsonObject().apply {
        addProperty("root", true)
    })
}

private suspend fun RoutingContext.getHello() {
    val name = parameters["name"] ?: queryParameters["name"] ?: "World"
    respond(JsonObject().apply {
        addProperty("message", "Hello, $name!")
    })
}

private suspend fun RoutingContext.getHelloWithAge() {
    val name = parameters["name"] ?: "World"
    val age = parameters["age"] ?: "0"
    respond(JsonObject().apply {
        addProperty("message", "Hello, $name! You are $age years old.")
    })
}
