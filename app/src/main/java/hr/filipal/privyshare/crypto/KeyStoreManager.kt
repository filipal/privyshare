package hr.filipal.privyshare.crypto

interface KeyStoreManager {

    data class PublicKeys(
        val signEd25519: ByteArray, // javni ključ za potpis
        val kexX25519: ByteArray    // javni ključ za razmjenu
    )

    /** Ako ne postoje identitetni ključevi, generiraj ih i trajno pohrani (ThisDeviceOnly). */
    fun ensureIdentityKeys()

    /** Vrati javne ključeve korisnika. */
    fun getPublicKeys(): PublicKeys

    /** Ed25519 potpis. */
    fun sign(data: ByteArray): ByteArray

    /** Ed25519 verifikacija. */
    fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean

    /** X25519: omotaj CEK za primatelja (crypto_box_seal ekvivalent). */
    fun sealCEK(cek: ByteArray, recipientPubX25519: ByteArray): ByteArray

    /** X25519: rasomotaj CEK za mene. */
    fun unsealCEK(sealed: ByteArray): ByteArray
}
