package hr.filipal.privyshare.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Pohrana tajnog 32B seed-a koristeći Android Keystore (AES/GCM, bez deprecated API-ja).
 * - AES ključ (256b) se generira i čuva u AndroidKeyStore pod aliasom.
 * - Seed se enkriptira AES/GCM-om i zapisuje u filesDir kao [IV || CIPHERTEXT].
 * - Ovo je "ThisDeviceOnly": nema cloud backupa ključa.
 *
 * Kripto operacije (Ed25519/X25519) ćemo dodati kasnije kada ubacimo libsodium.
 */
class AndroidKeyStoreManager(private val appContext: Context) : KeyStoreManager {

    private val alias = "privyshare_master_aes"
    private val seedFile: File get() = File(appContext.filesDir, "identity_seed.bin")

    // === Public API ===

    override fun ensureIdentityKeys() {
        ensureAesKey()
        if (!seedFile.exists()) {
            val seed = ByteArray(32).also { SecureRandom().nextBytes(it) }
            saveEncryptedSeed(seed)
        }
    }

    override fun getPublicKeys(): KeyStoreManager.PublicKeys {
        // TODO: derivirati javne ključeve iz seed-a (libsodium) → ed25519 + x25519
        throw NotImplementedError("getPublicKeys: implementirati nakon dodavanja libsodiuma")
    }

    override fun sign(data: ByteArray): ByteArray {
        // TODO: ed25519_sign(priv ključ deriviran iz seed-a)
        throw NotImplementedError("sign: implementirati nakon dodavanja libsodiuma")
    }

    override fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        // TODO: ed25519_verify
        throw NotImplementedError("verify: implementirati nakon dodavanja libsodiuma")
    }

    override fun sealCEK(cek: ByteArray, recipientPubX25519: ByteArray): ByteArray {
        // TODO: crypto_box_seal
        throw NotImplementedError("sealCEK: implementirati nakon dodavanja libsodiuma")
    }

    override fun unsealCEK(sealed: ByteArray): ByteArray {
        // TODO: crypto_box_seal_open
        throw NotImplementedError("unsealCEK: implementirati nakon dodavanja libsodiuma")
    }

    // === Private helpers ===

    private fun ensureAesKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = (ks.getKey(alias, null) as? SecretKey)
        if (existing != null) return existing

        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // Ako želiš tražiti biometriju za pristup:
            // .setUserAuthenticationRequired(true)
            // .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            .build()

        kg.init(spec)
        return kg.generateKey()
    }

    private fun getAesKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return ks.getKey(alias, null) as SecretKey
    }

    private fun saveEncryptedSeed(seed: ByteArray) {
        val key = getAesKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv // 12B
        val ciphertext = cipher.doFinal(seed)

        // Spremi [IV || CIPHERTEXT]
        seedFile.parentFile?.mkdirs()
        seedFile.outputStream().use { out ->
            out.write(iv)
            out.write(ciphertext)
        }
    }

    /** Dekriptiraj i vrati 32B seed iz filesDir-a. */
    private fun readSeed(): ByteArray {
        require(seedFile.exists()) { "Identity seed not initialized. Call ensureIdentityKeys() first." }
        val bytes = seedFile.readBytes()
        require(bytes.size > 12) { "Corrupted seed file." }
        val iv = bytes.copyOfRange(0, 12)
        val ct = bytes.copyOfRange(12, bytes.size)

        val key = getAesKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }
}
