package hr.filipal.privyshare.io

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object EncFileManager {
    fun encCacheDir(context: Context): File =
        File(context.cacheDir, "enc").apply { mkdirs() }

    fun newEncFile(context: Context, baseName: String = "payload"): File =
        File(encCacheDir(context), "$baseName-${System.currentTimeMillis()}.enc")

    fun shareEnc(context: Context, file: File, chooserTitle: String = "Send encrypted file") {
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}