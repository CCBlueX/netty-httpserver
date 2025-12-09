import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk

suspend fun main() {
    val server = HttpServer()

    server.routing {
        get("/", ::getRoot)
        get("/hello", ::getHello) // /hello?name=World
        get("/hello/:name", ::getHello) // /hello/World
        get("/hello/:name/:age", ::getHelloWithAge) // /hello/World/20
    }

    server.start(8080)  // Start the server on port 8080
}

@Suppress("UNUSED_PARAMETER")
fun getRoot(requestObject: RequestObject): FullHttpResponse {
    return httpOk(JsonObject().apply {
        addProperty("root", true)
    })
}

fun getHello(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.params["name"] ?: requestObject.queryParams["name"] ?: "World"
    return httpOk(JsonObject().apply {
        addProperty("message", "Hello, $name!")
    })
}

fun getHelloWithAge(requestObject: RequestObject): FullHttpResponse {
    val name = requestObject.params["name"] ?: "World"
    val age = requestObject.params["age"] ?: "0"
    return httpOk(JsonObject().apply {
        addProperty("message", "Hello, $name! You are $age years old.")
    })
}

