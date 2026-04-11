package com.undersky.androidim

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.undersky.androidim.data.AuthTokenHolder
import com.undersky.androidim.databinding.ActivityMainBinding
import com.undersky.androidim.notify.PendingChatNavigation
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val keepSplashScreen = AtomicBoolean(true)

    private val requestPostNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun endSplashHold() {
        keepSplashScreen.set(false)
    }

    fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSplashScreen.get() }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        consumeLaunchChatIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchChatIntent(intent)
    }

    private fun consumeLaunchChatIntent(intent: Intent?) {
        if (intent == null) return
        val peer = intent.getLongExtra(EXTRA_OPEN_PEER_USER_ID, -1L)
        val group = intent.getLongExtra(EXTRA_OPEN_GROUP_ID, -1L)
        if (peer <= 0L && group <= 0L) return
        val title = intent.getStringExtra(EXTRA_OPEN_TITLE_FALLBACK).orEmpty()
        (application as ImApp).pendingChatNavigation = PendingChatNavigation(peer, group, title)
    }

    companion object {
        const val EXTRA_OPEN_PEER_USER_ID = "open_peer_user_id"
        const val EXTRA_OPEN_GROUP_ID = "open_group_id"
        const val EXTRA_OPEN_TITLE_FALLBACK = "open_title_fallback"
    }
}
