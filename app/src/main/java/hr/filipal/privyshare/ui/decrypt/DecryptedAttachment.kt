package hr.filipal.privyshare.ui.decrypt

import android.net.Uri

data class DecryptedAttachment(
    val name: String,
    val mime: String,
    val uri: Uri
)
