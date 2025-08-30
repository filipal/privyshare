package hr.filipal.privyshare.ui.keys

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text

@Composable
fun VerifyBottomSheet(
    sasWords: List<String>, sasNumbers: String,
    onScanQr: () -> Unit, onCopySas: () -> Unit,
    onMatch: () -> Unit, onMismatch: () -> Unit,
    onEnableNfc: () -> Unit
) {
    Text("Verify (QR • SAS • NFC)")
}