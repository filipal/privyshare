package hr.filipal.privyshare.crypto

import com.goterl.lazysodium.SodiumAndroid
import com.goterl.lazysodium.LazySodiumAndroid
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom

class SodiumCryptoEngine : CryptoEngine {

    private val sodium = SodiumAndroid()
    @Suppress("unused")
    private val lazy = LazySodiumAndroid(sodium, Charsets.UTF_8) // učita native lib
    private val random = SecureRandom()

    // XChaCha20-Poly1305-IETF fiksne veličine
    private companion object {
        const val KEYBYTES = 32      // crypto_aead_xchacha20poly1305_ietf_KEYBYTES
        const val NPUBBYTES = 24     // crypto_aead_xchacha20poly1305_ietf_NPUBBYTES
        const val ABYTES = 16        // crypto_aead_xchacha20poly1305_ietf_ABYTES
    }

    override fun generateCEK(): ByteArray =
        ByteArray(KEYBYTES).also { random.nextBytes(it) }

    override suspend fun encryptStream(
        input: InputStream,
        output: OutputStream,
        cek: ByteArray
    ): String {
        require(cek.size == KEYBYTES) { "CEK must be $KEYBYTES bytes" }

        val plain = input.readBytes()

        val nonce = ByteArray(NPUBBYTES).also { random.nextBytes(it) }
        val cipher = ByteArray(plain.size + ABYTES)
        val outLen = LongArray(1)

        val ok = sodium.crypto_aead_xchacha20poly1305_ietf_encrypt(
            cipher, outLen,
            plain, plain.size.toLong(),
            null, 0L,          // AAD
            null,              // nsec
            nonce, cek
        )
        require(ok == 0) { "Encryption failed" }

        // Zapiši [nonce || cipher]
        output.write(nonce)
        output.write(cipher)

        // Hashiramo SAMO ciphertext (bez nonce-a), prema našoj specifikaciji
        return EnvelopeCodec.sha256Hex(cipher.inputStream())
    }

    override suspend fun decryptStream(
        input: InputStream,
        output: OutputStream,
        cek: ByteArray
    ) {
        require(cek.size == KEYBYTES) { "CEK must be $KEYBYTES bytes" }

        val all = input.readBytes()
        require(all.size > NPUBBYTES + ABYTES) { "Invalid ciphertext" }

        val nonce  = all.copyOfRange(0, NPUBBYTES)
        val cipher = all.copyOfRange(NPUBBYTES, all.size)

        val plain = ByteArray(cipher.size - ABYTES)
        val outLen = LongArray(1)

        val ok = sodium.crypto_aead_xchacha20poly1305_ietf_decrypt(
            plain, outLen,
            null,
            cipher, cipher.size.toLong(),
            null, 0L,
            nonce, cek
        )
        require(ok == 0) { "Decryption failed" }

        output.write(plain, 0, outLen[0].toInt())
    }
}
