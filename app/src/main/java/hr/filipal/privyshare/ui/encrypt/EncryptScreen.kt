package hr.filipal.privyshare.ui.encrypt

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import hr.filipal.privyshare.crypto.*
import hr.filipal.privyshare.io.EncFileManager
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf(TextFieldValue("")) }
    val attachments = remember { mutableStateListOf<Uri>() }

    // Photo Picker (slike/video) – bez runtime permissiona
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) attachments.addAll(uris)
    }

    // SAF Document picker (PDF/ostalo)
    val docPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) attachments.addAll(uris)
    }
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Encrypt & Share") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            // 0) pripremi izlazni .enc fajl
                            val outFile = EncFileManager.newEncFile(context, "payload")

                            // 1) payload = samo tekst poruke (MVP)
                            val payloadBytes = message.text.toByteArray(Charsets.UTF_8)
                            val payloadIn = ByteArrayInputStream(payloadBytes)

                            // 2) šifriraj tijelo
                            val engine = SodiumCryptoEngine()
                            val cek = engine.generateCEK()
                            val bodyBaos = ByteArrayOutputStream()

                            val bodyHashHex = engine.encryptStream(
                                input = payloadIn,
                                output = bodyBaos,
                                cek = cek
                            )
                            val cipherBody = bodyBaos.toByteArray()

                            // 3) header meta + (zasad prazni) recipients i signature
                            val meta = EncMeta(
                                mime = "text/plain",
                                createdAt = EnvelopeCodec.nowIsoUtc(),
                                app = "PrivyShare/1.0",
                                hasText = message.text.isNotEmpty(),
                                attachments = attachments.map { uri ->
                                    EncAttachment(
                                        name = uri.lastPathSegment ?: "file",
                                        mime = context.contentResolver.getType(uri) ?: "*/*"
                                    )
                                }
                            )
                            val recipients: List<EncRecipient> = emptyList()
                            val fakeSig = ""

                            val header = EnvelopeCodec.buildHeader(
                                meta = meta,
                                recipients = recipients,
                                bodyHashHex = bodyHashHex,
                                signatureB64 = fakeSig
                            )
                            val headerJson = EnvelopeCodec.toJsonBytes(header)

                            // 4) upiši [header]\n[nonce||cipher] u .enc
                            outFile.outputStream().use { os ->
                                os.write(headerJson)
                                os.write('\n'.code)
                                os.write(cipherBody)
                            }

                            // 5) Share Sheet
                            EncFileManager.shareEnc(context, outFile, "Send encrypted file")
                        } catch (e: IllegalArgumentException) {
                            Log.e("EncryptScreen", "Encryption failed", e)
                            snackbar.showSnackbar("Encryption failed")
                            return@launch
                        } catch (e: UnsatisfiedLinkError) {
                            Log.e("EncryptScreen", "Encryption failed", e)
                            snackbar.showSnackbar("Encryption failed")
                            return@launch
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) { Text("Encrypt & Share") }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Write your secure message…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Attach photos/videos") }

                OutlinedButton(
                    onClick = { docPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f)
                ) { Text("Attach documents") }
            }
            Spacer(Modifier.height(16.dp))

            Text("Attachments (${attachments.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(attachments) { uri ->
                    ListItem(
                        headlineContent = { Text(uri.lastPathSegment ?: uri.toString()) },
                        supportingContent = { Text(uri.toString(), maxLines = 1) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
