import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk

suspend fun main() {
    val server = HttpServer()

    server.routing {
        post("/echo", ::postEcho) // /echo
    }

    server.start(8080)  // Start the server on port 8080
}

fun postEcho(requestObject: RequestObject): FullHttpResponse {
    return httpOk(requestObject.asJson<JsonObject>())
}