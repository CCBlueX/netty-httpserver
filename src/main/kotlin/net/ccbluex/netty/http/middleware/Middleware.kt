package net.ccbluex.netty.http.middleware

import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.netty.http.model.RequestContext

fun interface Middleware {
    operator fun invoke(context: RequestContext, response: FullHttpResponse): FullHttpResponse
}