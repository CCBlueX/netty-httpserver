import io.netty.handler.codec.http.*
import com.google.gson.JsonObject
import net.ccbluex.netty.http.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class HttpResponseUtilTest {

    @Test
    fun httpResponse_createsResponseWithCorrectStatusAndContent() {
        val response = httpResponse(HttpResponseStatus.OK, "text/plain", "Hello, World!")
        assertEquals(HttpResponseStatus.OK, response.status())
        assertEquals("text/plain", response.headers()[HttpHeaderNames.CONTENT_TYPE])
        assertEquals("Hello, World!", response.content().toString(Charsets.UTF_8))
    }

    @Test
    fun httpResponse_createsJsonResponse() {
        val json = JsonObject().apply { addProperty("key", "value") }
        val response = httpResponse(HttpResponseStatus.OK, json)
        assertEquals(HttpResponseStatus.OK, response.status())
        assertEquals("application/json", response.headers()[HttpHeaderNames.CONTENT_TYPE])
        assertEquals("{\"key\":\"value\"}", response.content().toString(Charsets.UTF_8))
    }

    @Test
    fun httpOk_creates200Response() {
        val json = JsonObject().apply { addProperty("key", "value") }
        val response = httpOk(json)
        assertEquals(HttpResponseStatus.OK, response.status())
        assertEquals("application/json", response.headers()[HttpHeaderNames.CONTENT_TYPE])
        assertEquals("{\"key\":\"value\"}", response.content().toString(Charsets.UTF_8))
    }

    @Test
    fun httpNotFound_creates404Response() {
        val response = httpNotFound("/path", "Not Found")
        assertEquals(HttpResponseStatus.NOT_FOUND, response.status())
        assertEquals("application/json", response.headers()[HttpHeaderNames.CONTENT_TYPE])
        assertEquals("{\"path\":\"/path\",\"reason\":\"Not Found\"}", response.content().toString(Charsets.UTF_8))
    }

    @Test
    fun httpInternalServerError_creates500Response() {
        val response = httpInternalServerError("Exception occurred")
        assertEquals(HttpResponseStatus.INTERNAL_SERVER_ERROR, response.status())
        assertEquals("application/json", response.headers()[HttpHeaderNames.CONTENT_TYPE])
        assertEquals("{\"reason\":\"Exception occurred\"}", response.content().toString(Charsets.UTF_8))
    }

    @Test
    fun httpForbidden_creates403Response() {
        val response = httpForbidden("Forbidden")
        assertEquals(HttpResponseStatus.FORBIDDEN, response.status())
        assertEquals("application/json", response.headers()[HttpHeaderNames.CONTENT_TYPE])
        assertEquals("{\"reason\":\"Forbidden\"}", response.content().toString(Charsets.UTF_8))
    }

    @Test
    fun httpBadRequest_creates400Response() {
        val response = httpBadRequest("Bad Request")
        assertEquals(HttpResponseStatus.BAD_REQUEST, response.status())
        assertEquals("application/json", response.headers()[HttpHeaderNames.CONTENT_TYPE])
        assertEquals("{\"reason\":\"Bad Request\"}", response.content().toString(Charsets.UTF_8))
    }

    @Test
    fun httpFile_createsResponseWithFileContent() {
        val file = File("test.txt").apply { writeText("File content") }
        val response = httpFile(file)
        assertEquals(HttpResponseStatus.OK, response.status())
        assertEquals("text/plain", response.headers()[HttpHeaderNames.CONTENT_TYPE])
        assertEquals("File content", response.content().toString(Charsets.UTF_8))
        file.delete()
    }

    @Test
    fun httpFileStream_createsResponseWithStreamContent() {
        val stream = "Stream content".byteInputStream()
        val response = httpFileStream(stream)
        assertEquals(HttpResponseStatus.OK, response.status())
        assertEquals("text/plain", response.headers()[HttpHeaderNames.CONTENT_TYPE])
        assertEquals("Stream content", response.content().toString(Charsets.UTF_8))
    }
}