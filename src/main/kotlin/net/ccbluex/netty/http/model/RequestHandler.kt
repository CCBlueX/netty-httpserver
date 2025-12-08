package net.ccbluex.netty.http.model

import io.netty.handler.codec.http.FullHttpResponse

fun interface RequestHandler {
    suspend fun handle(request: RequestObject): FullHttpResponse
}
