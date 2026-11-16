package com.example

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.StoreRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainShopScreen
import com.example.ui.screens.StoreOnboardingScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.StoreOSTheme
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.LocalAppLanguage
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        applyImmersiveMode()

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        val repository = StoreRepository(this)

        setContent {
            StoreOSTheme {
                val authViewModel = remember { AuthViewModel(repository) }
                AppRoot(
                    authViewModel = authViewModel,
                    repository = repository
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveMode()
        }
    }

    private fun applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

@Composable
fun AppRoot(
    authViewModel: AuthViewModel,
    repository: StoreRepository
) {
    val uiState by authViewModel.uiState.collectAsState()
    val currentLanguage by authViewModel.currentLanguage.collectAsState()

    CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
        when (val state = uiState) {
        is AuthUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = VividGreen,
                    strokeWidth = 3.dp
                )
            }
        }

        is AuthUiState.Unauthenticated -> {
            AuthScreen(authViewModel = authViewModel)
        }

        is AuthUiState.AuthenticatedNoStore -> {
            StoreOnboardingScreen(authViewModel = authViewModel)
        }

        is AuthUiState.AuthenticatedWithStore -> {
            // Keyed by uid and storeId to ensure zero state leaks across accounts or stores
            key(state.user.uid, state.storeId) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val networkMonitor = remember { com.example.util.NetworkMonitor(context) }
                val storeViewModel = remember(state.user.uid, state.storeId) {
                    StoreViewModel(state.storeId, repository, networkMonitor, currentUserId = state.user.uid)
                }
                MainShopScreen(
                    authViewModel = authViewModel,
                    storeViewModel = storeViewModel
                )
            }
        }

        is AuthUiState.DemoMode -> {
            key("demo_user", state.storeId) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val networkMonitor = remember { com.example.util.NetworkMonitor(context) }
                val storeViewModel = remember(state.storeId) {
                    StoreViewModel(state.storeId, repository, networkMonitor, currentUserId = "demo_user")
                }
                MainShopScreen(
                    authViewModel = authViewModel,
                    storeViewModel = storeViewModel
                )
            }
        }
        }
    }
}
