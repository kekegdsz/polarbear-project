package com.undersky.androidim

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.undersky.androidim.data.AuthTokenHolder
import com.undersky.androidim.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val keepSplashScreen = AtomicBoolean(true)

    fun endSplashHold() {
        keepSplashScreen.set(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSplashScreen.get() }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        val app = application as ImApp
        lifecycleScope.launch {
            app.sessionStore.sessionFlow.collect { session ->
                AuthTokenHolder.set(session?.token)
                if (session != null) {
                    app.imSocket.connect(session.userId)
                } else {
                    app.imSocket.disconnect(clearUser = true)
                }
            }
        }
    }
}
