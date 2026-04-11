package com.undersky.androidim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.undersky.androidim.data.AuthTokenHolder
import com.undersky.androidim.data.UserSession
import com.undersky.androidim.ui.navigation.ImRootNav
import com.undersky.androidim.ui.theme.WxNav
import com.undersky.androidim.ui.theme.WxTheme
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    private val keepSplashScreen = AtomicBoolean(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSplashScreen.get() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        val app = application as ImApp
        setContent {
            var session by remember { mutableStateOf<UserSession?>(null) }
            var sessionReady by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                app.sessionStore.sessionFlow.collect { s ->
                    session = s
                    if (!sessionReady) {
                        sessionReady = true
                        keepSplashScreen.set(false)
                    }
                }
            }

            LaunchedEffect(session) {
                AuthTokenHolder.set(session?.token)
            }
            LaunchedEffect(session?.userId) {
                if (session != null) {
                    app.imSocket.connect(session!!.userId)
                } else {
                    app.imSocket.disconnect(clearUser = true)
                }
            }

            WxTheme {
                if (!sessionReady) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(WxNav)
                    )
                } else {
                    ImRootNav(
                        session = session,
                        app = app,
                        startDestination = if (session != null) "main" else "login"
                    )
                }
            }
        }
    }
}
