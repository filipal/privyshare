package hr.filipal.privyshare

import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun envelopeTimestamp_isUtcIso() {
        val t = hr.filipal.privyshare.crypto.EnvelopeCodec.nowIsoUtc()
        assertTrue(t.endsWith("Z"))   // npr. 2025-01-10T12:34:56Z
    }
}