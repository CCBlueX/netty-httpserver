package util

import net.ccbluex.netty.http.util.readAsBase64
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class Base64Test {

    @Test
    fun `readAsBase64 should return correct Base64 string`() {
        val content = Random.nextBytes(128)
        val tempFile: Path = Files.createTempFile("test-image", ".bin")
        Files.write(tempFile, content)

        val resultBase64 = tempFile.readAsBase64()

        val expectedBase64 = Base64.getEncoder().encodeToString(content)
        assertEquals(expectedBase64, resultBase64)
        try {
            Files.deleteIfExists(tempFile)
        } catch (e: java.nio.file.AccessDeniedException) {}
    }

}