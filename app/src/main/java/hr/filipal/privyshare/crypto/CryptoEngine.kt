package hr.filipal.privyshare.crypto

import java.io.InputStream
import java.io.OutputStream

interface CryptoEngine {
    fun generateCEK(): ByteArray
    suspend fun encryptStream(input: InputStream, output: OutputStream, cek: ByteArray): String // returns bodyHashHex
    suspend fun decryptStream(input: InputStream, output: OutputStream, cek: ByteArray)
}