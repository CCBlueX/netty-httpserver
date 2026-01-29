package net.ccbluex.netty.http.middleware

import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaders
import net.ccbluex.netty.http.HttpServer.Companion.logger
import net.ccbluex.netty.http.model.RequestContext
import java.net.URI
import java.net.URISyntaxException

/**
 * Middleware to handle Cross-Origin Resource Sharing (CORS) requests.
 *
 * @param allowedOrigins List of allowed (host) origins (default: localhost, 127.0.0.1)
 *   - If we want to specify a protocol and port, we should use the full origin (e.g., http://localhost:8080).
 * @param allowedMethods List of allowed HTTP methods (default: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)
 * @param allowedHeaders List of allowed HTTP headers (default: Content-Type, Content-Length, Authorization, Accept, X-Requested-With)
 *
 * @see RequestContext
 */
class CorsMiddleware(
    private val allowedOrigins: List<String> =
        listOf("localhost", "127.0.0.1"),
    private val allowedMethods: List<String> =
        listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"),
    private val allowedHeaders: List<String> =
        listOf("Content-Type", "Content-Length", "Authorization", "Accept", "X-Requested-With")
): Middleware.OnResponse {

    /**
     * Middleware to handle CORS requests.
     * Pass to server.middleware() to apply the CORS policy to all requests.
     */
    override fun invoke(context: RequestContext, response: FullHttpResponse): FullHttpResponse {
        val httpHeaders = response.headers()
        val requestOrigin = context.headers[HttpHeaderNames.ORIGIN]
        val allowedOrigin = if (allowedOrigins.contains("*")) {
            "*"
        } else if (requestOrigin != null) {
            val host = try {
                URI(requestOrigin).host
            } catch (e: Exception) {
                logger.error("Invalid Origin header: $requestOrigin", e)
                null
            }

            if (host != null && allowedOrigins.contains(host) || allowedOrigins.contains(requestOrigin)) {
                requestOrigin
            } else {
                null
            }
        } else {
            null
        }

        if (allowedOrigin == null) {
            logger.debug("CORS origin not allowed: $requestOrigin")
            return response
        }
        httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN] = allowedOrigin
        httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS] = allowedMethods.joinToString(", ")
        httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS] = allowedHeaders.joinToString(", ")
        return response
    }

}