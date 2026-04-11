package com.undersky.androidim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.view.WindowCompat
import com.undersky.androidim.data.UserSession
import com.undersky.androidim.ui.navigation.ImRootNav
import com.undersky.androidim.ui.theme.WxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        val app = application as ImApp
        setContent {
            val session by produceState<UserSession?>(initialValue = null) {
                app.sessionStore.sessionFlow.collect { value = it }
            }
            LaunchedEffect(session?.userId) {
                if (session != null) {
                    app.imSocket.connect(session!!.userId)
                } else {
                    app.imSocket.disconnect(clearUser = true)
                }
            }
            WxTheme {
                ImRootNav(
                    session = session,
                    app = app
                )
            }
        }
    }
}
