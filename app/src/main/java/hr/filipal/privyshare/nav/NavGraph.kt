package hr.filipal.privyshare.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import hr.filipal.privyshare.ui.encrypt.EncryptScreen
import hr.filipal.privyshare.ui.home.HomeScreen
import hr.filipal.privyshare.ui.keys.KeysSettingsScreen

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onEncrypt = { nav.navigate("encrypt") },
                onKeys = { nav.navigate("keys") }
            )
        }
        composable("encrypt") { EncryptScreen() }
        composable("keys") { KeysSettingsScreen() }
    }
}