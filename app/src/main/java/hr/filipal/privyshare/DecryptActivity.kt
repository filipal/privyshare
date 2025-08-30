package hr.filipal.privyshare

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

class DecryptActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Provjeri je li nam stigao .enc fajl preko Intenta
        val incoming: Uri? = intent.data ?: intent.clipData?.getItemAt(0)?.uri

        setContent {
            MaterialTheme {
                if (incoming != null) {
                    Text(text = "Received .enc file: ${incoming.lastPathSegment ?: incoming}")
                } else {
                    Text(text = "No .enc file received")
                }
            }
        }

        // Ovdje kasnije dodaš:
        // val inStream = contentResolver.openInputStream(incoming!!)
        // decryptStream(inStream, ...)
    }
}
