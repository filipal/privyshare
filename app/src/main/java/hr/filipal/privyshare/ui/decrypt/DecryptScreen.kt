package hr.filipal.privyshare.ui.decrypt

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecryptScreen(
    sourceUri: Uri,
    onStartDecrypt: () -> Unit = {},            // pokreni dekripciju
    isDecrypting: Boolean = false,
    messagePreview: String? = null,             // tekst iz paketa, ako postoji
    attachments: List<DecryptedAttachment> = emptyList(), // dekriptirani prilozi
    onOpenAttachment: (DecryptedAttachment) -> Unit = {}, // "View only"
    onExportAttachment: (DecryptedAttachment) -> Unit = {} // ⚠️ Export (decrypted)
) {
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Open & Decrypt") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Received file:", style = MaterialTheme.typography.titleSmall)
            Text(
                text = sourceUri.lastPathSegment ?: sourceUri.toString(),
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )

            if (messagePreview != null) {
                HorizontalDivider()
                Text("Message:", style = MaterialTheme.typography.titleSmall)
                Text(messagePreview)
            }

            if (attachments.isNotEmpty()) {
                HorizontalDivider()
                Text("Attachments (${attachments.size})", style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attachments) { att ->
                        ListItem(
                            leadingContent = {
                                // mali thumbnail za slike, inače placeholder
                                if (att.mime.startsWith("image/")) {
                                    AsyncImage(
                                        model = att.uri,
                                        contentDescription = att.name,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    // možeš ovdje staviti ikonicu za 'doc'
                                    Spacer(Modifier.size(40.dp))
                                }
                            },
                            headlineContent = {
                                Text(att.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Text(att.mime, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onOpenAttachment(att) }) {
                                        Text("View")
                                    }
                                    // ⚠️ Export skida zaštitu – naglasi kasnije u UI
                                    Button(onClick = { onExportAttachment(att) }) {
                                        Text("Export")
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }

            Button(
                onClick = onStartDecrypt,
                enabled = !isDecrypting,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isDecrypting) "Decrypting…" else "Start Decrypt (stub)") }

            if (isDecrypting) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                }
            }
        }
    }
}
