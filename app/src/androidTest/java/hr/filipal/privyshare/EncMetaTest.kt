package hr.filipal.privyshare

import hr.filipal.privyshare.crypto.EncAttachment
import hr.filipal.privyshare.crypto.EncMeta
import hr.filipal.privyshare.crypto.EnvelopeCodec
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

class EncMetaTest {

    private fun metaJson(meta: EncMeta): JsonObject {
        val aadB64 = EnvelopeCodec.computeAadB64(meta, emptyList())
        val aadBytes = Base64.getDecoder().decode(aadB64)
        val root = Json.parseToJsonElement(String(aadBytes, StandardCharsets.UTF_8)).jsonObject
        return root["meta"]!!.jsonObject
    }

    @Test
    fun textOnlyMeta() {
        val meta = EncMeta("text/plain", "2024-01-01T00:00:00Z", "Test", hasText = true)
        val obj = metaJson(meta)
        assertEquals("text/plain", obj["mime"]!!.jsonPrimitive.content)
        assertTrue(obj["has_text"]!!.jsonPrimitive.boolean)
        assertFalse(obj.containsKey("attachments"))
    }

    @Test
    fun attachmentsOnlyMeta() {
        // Ako EncAttachment NEMA 'bytes' polje, koristi samo (name, mime) i ukloni zadnji assert
        val meta = EncMeta(
            mime = "multipart/mixed",
            createdAt = "2024-01-01T00:00:00Z",
            app = "Test",
            attachments = listOf(EncAttachment("a.txt", "text/plain", 5))
        )
        val obj = metaJson(meta)
        assertFalse(obj.containsKey("has_text"))
        val arr = obj["attachments"]!!.jsonArray
        val att0 = arr[0].jsonObject
        assertEquals("a.txt", att0["name"]!!.jsonPrimitive.content)
        assertEquals("text/plain", att0["mime"]!!.jsonPrimitive.content)
    }

    @Test
    fun textAndAttachmentsMeta() {
        val meta = EncMeta(
            mime = "multipart/mixed",
            createdAt = "2024-01-01T00:00:00Z",
            app = "Test",
            hasText = true,
            attachments = listOf(EncAttachment("a.txt", "text/plain", 5))
        )
        val obj = metaJson(meta)
        assertTrue(obj["has_text"]!!.jsonPrimitive.boolean)
        val arr = obj["attachments"]!!.jsonArray
        assertEquals(1, arr.size)
    }
}
