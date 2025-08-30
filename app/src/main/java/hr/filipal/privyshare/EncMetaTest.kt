package hr.filipal.privyshare

import android.util.Base64
import hr.filipal.privyshare.crypto.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.long
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.*
import org.junit.Test

class EncMetaTest {
    private fun metaJson(meta: EncMeta): JsonObject {
        val aadB64 = EnvelopeCodec.computeAadB64(meta, emptyList())
        val aadBytes = Base64.decode(aadB64, Base64.NO_WRAP)
        val root = Json.parseToJsonElement(String(aadBytes)).jsonObject
        return root["meta"]!!.jsonObject
    }

    @Test
    fun textOnlyMeta() {
        val meta = EncMeta(
            mime = "text/plain",
            createdAt = "2024-01-01T00:00:00Z",
            app = "Test",
            hasText = true
        )
        val obj = metaJson(meta)
        assertEquals("text/plain", obj["mime"]!!.jsonPrimitive.content)
        assertTrue(obj["has_text"]!!.jsonPrimitive.boolean)
        assertFalse(obj.containsKey("attachments"))
    }

    @Test
    fun attachmentsOnlyMeta() {
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
        assertEquals(5L, att0["bytes"]!!.jsonPrimitive.long)
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