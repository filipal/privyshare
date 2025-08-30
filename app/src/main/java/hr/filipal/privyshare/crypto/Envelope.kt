package hr.filipal.privyshare.crypto

import android.util.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ENC Header (v2) — Kotlin camelCase imena + @SerialName za JSON snake_case.
 */
@Serializable
data class EncAlgorithms(
    val aead: String = "xchacha20poly1305",
    val kex: String = "x25519",
    val sig: String = "ed25519"
)

@Serializable
data class EncAttachment(
    val name: String,
    val mime: String,
    val bytes: Long
)

@Serializable
data class EncMeta(
    val mime: String,                           // npr. "multipart/mixed"
    @SerialName("created_at") val createdAt: String, // ISO8601 (UTC)
    val app: String,                            // npr. "PrivyShare/0.1"
    @SerialName("has_text") val hasText: Boolean = false,
    val attachments: List<EncAttachment> = emptyList()
)

@Serializable
data class EncRecipient(
    val id: String,                             // npr. "fp:ABCD1234..."
    @SerialName("wrapped_cek") val wrappedCek: String // base64(CEK omotan za recipienta)
)

@Serializable
data class EncHeader(
    @SerialName("enc_v") val encV: Int = 2,
    val algs: EncAlgorithms = EncAlgorithms(),
    val meta: EncMeta,
    val recipients: List<EncRecipient>,
    val aad: String,                            // base64(canonical_json(meta+recipients))
    @SerialName("body_hash") val bodyHash: String, // hex(SHA-256 over ciphertext body)
    val signature: String                       // base64(ed25519_sign(aad || body_hash))
)

/**
 * EnvelopeCodec — izgradnja headera, AAD, hash utility, (de)serialize.
 */
@Suppress("unused") // neke funkcije će postati 'used' kad spojimo kripto i UI
object EnvelopeCodec {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = false
        encodeDefaults = false
        explicitNulls = false
    }

    /** ISO8601 UTC "now" helper (radi na minSdk 24) */
    fun nowIsoUtc(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * AAD = BASE64( canonical_json(meta+recipients) )
     * Kanonizacija: ručno složimo JsonObject s determinističkim poretkom ključeva.
     */
    fun computeAadB64(meta: EncMeta, recipients: List<EncRecipient>): String {
        val metaObj = buildJsonObject {
            put("mime", JsonPrimitive(meta.mime))
            put("created_at", JsonPrimitive(meta.createdAt))
            put("app", JsonPrimitive(meta.app))
            if (meta.hasText) put("has_text", JsonPrimitive(true))
            if (meta.attachments.isNotEmpty()) {
                put("attachments", buildJsonArray {
                    meta.attachments.forEach { att ->
                        add(buildJsonObject {
                            put("name", JsonPrimitive(att.name))
                            put("mime", JsonPrimitive(att.mime))
                            put("bytes", JsonPrimitive(att.bytes))
                        })
                    }
                })
            }
        }

        val recArr = buildJsonArray {
            recipients.forEach { r ->
                add(buildJsonObject {
                    put("id", JsonPrimitive(r.id))
                    put("wrapped_cek", JsonPrimitive(r.wrappedCek))
                })
            }
        }

        val aadObj = buildJsonObject {
            put("meta", metaObj)
            put("recipients", recArr)
        }

        val bytes = json.encodeToString(JsonObject.serializer(), aadObj)
            .toByteArray(Charsets.UTF_8)
        return base64(bytes)
    }

    /** HEX(SHA-256) nad cijelim ciphertext bodyjem (stream). */
    fun sha256Hex(input: InputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buf)
            if (read <= 0) break
            md.update(buf, 0, read)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Izgradi header iz meta+recipients+bodyHash; potpis setaj naknadno ili proslijedi. */
    fun buildHeader(
        meta: EncMeta,
        recipients: List<EncRecipient>,
        bodyHashHex: String,
        signatureB64: String
    ): EncHeader {
        val aadB64 = computeAadB64(meta, recipients)
        return EncHeader(
            encV = 2,
            algs = EncAlgorithms(),
            meta = meta,
            recipients = recipients,
            aad = aadB64,
            bodyHash = bodyHashHex,
            signature = signatureB64
        )
    }

    /** Serialize header u kompaktni JSON bytes. */
    fun toJsonBytes(header: EncHeader): ByteArray =
        json.encodeToString(EncHeader.serializer(), header).toByteArray(Charsets.UTF_8)

    /** Parse header iz JSON bytes. */
    fun parseHeader(bytes: ByteArray): EncHeader =
        json.decodeFromString(EncHeader.serializer(), bytes.toString(Charsets.UTF_8))

    /** Base64 helper (Android util, NO_WRAP) — radi na minSdk 24. */
    private fun base64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)
}
