package az.iptv.fplayer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import az.iptv.fplayer.ui.screen.AddPlaylistScreen
import az.iptv.fplayer.ui.screen.PlayerScreen
import az.iptv.fplayer.ui.theme.AppBg
import az.iptv.fplayer.ui.theme.FPLAYERTheme
import az.iptv.fplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            FPLAYERTheme {
                Surface(modifier = Modifier.fillMaxSize(), shape = RectangleShape) {
                    val snackbarHostState = remember { SnackbarHostState() }
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation()
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                        ExitHandler(snackbarHostState)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExitHandler(snackbarHostState: SnackbarHostState) {
    var lastBackTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackTime < 2000) {
            (context as? ComponentActivity)?.finishAffinity()
        } else {
            lastBackTime = currentTime
            // Burada snackbar göstərə bilərik
        }
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onFinish = { hasPlaylist ->
                val start = if (hasPlaylist) "player" else "add_playlist"
                navController.navigate(start) {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("add_playlist") {
            AddPlaylistScreen(
                onPlaylistLoaded = {
                    navController.navigate("player") {
                        popUpTo("add_playlist") { inclusive = true }
                    }
                },
                onBackToPlayer = {
                    navController.navigate("player") {
                        popUpTo("add_playlist") { inclusive = true }
                    }
                }
            )
        }
        composable("player") {
            PlayerScreen(
                onNavigateToPlaylist = {
                    navController.navigate("add_playlist")
                }
            )
        }
    }
}

@Composable
private fun SplashScreen(
    onFinish: (Boolean) -> Unit,
    vm: PlayerViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        val type = vm.prefs.playlistType.first()
        val hasPlaylist = type.isNotEmpty()
        delay(180)
        onFinish(hasPlaylist)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_fplayer_logo),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )
    }
}
