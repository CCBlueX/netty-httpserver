import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import net.ccbluex.netty.http.HttpServer
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class HttpServerTest {

    @Test
    fun broadcast_sendsMessageToAllActiveContexts() {
        val server = HttpServer()
        val ctx1 = mock(ChannelHandlerContext::class.java)
        val ctx2 = mock(ChannelHandlerContext::class.java)
        server.webSocketController.activeContexts.add(ctx1)
        server.webSocketController.activeContexts.add(ctx2)

        val message = TextWebSocketFrame("Test message")
        server.webSocketController.broadcast(message)

        verify(ctx1).writeAndFlush(message)
        verify(ctx2).writeAndFlush(message)
    }

    @Test
    fun broadcast_handlesExceptions() {
        val server = HttpServer()
        val ctx1 = mock(ChannelHandlerContext::class.java)
        val ctx2 = mock(ChannelHandlerContext::class.java)
        server.webSocketController.activeContexts.add(ctx1)
        server.webSocketController.activeContexts.add(ctx2)

        val message = TextWebSocketFrame("Test message")
        doThrow(RuntimeException::class.java).`when`(ctx1).writeAndFlush(message)

        server.webSocketController.broadcast(message)

        verify(ctx1).writeAndFlush(message)
        verify(ctx2).writeAndFlush(message)
    }

}