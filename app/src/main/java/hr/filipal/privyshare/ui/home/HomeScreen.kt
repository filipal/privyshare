package hr.filipal.privyshare.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onEncrypt: () -> Unit, onKeys: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = onEncrypt,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Encrypt & Share")
        }
        Button(
            onClick = onKeys,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Keys & Settings")
        }
    }
}
