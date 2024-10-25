package net.ccbluex.netty.http.middleware

import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.netty.http.model.RequestContext

typealias MiddlewareFunction = (RequestContext, FullHttpResponse) -> FullHttpResponse

interface Middleware {
    fun middleware(context: RequestContext, response: FullHttpResponse): FullHttpResponse
}