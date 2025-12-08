package net.ccbluex.netty.http.model

import io.netty.handler.codec.http.FullHttpResponse

fun interface RequestHandler {
    fun handle(request: RequestObject): FullHttpResponse
}
