package com.undersky.androidim

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.undersky.androidim.bootstrap.ImHostActivity
import com.undersky.androidim.bootstrap.PendingChatNavigation
import com.undersky.androidim.databinding.ActivityMainBinding
import com.undersky.androidim.notify.ImMessageNotifier
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity(), ImHostActivity {

    private lateinit var binding: ActivityMainBinding
    private val keepSplashScreen = AtomicBoolean(true)

    private val requestPostNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val services get() = (application as ImApp).services

    override fun endSplashHold() {
        keepSplashScreen.set(false)
    }

    override fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val navController by lazy {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHost.navController
    }

    override fun navigateSplashToMain() {
        navController.navigate(
            R.id.mainFragment,
            null,
            NavOptions.Builder().setPopUpTo(R.id.splashFragment, true).build()
        )
    }

    override fun navigateSplashToLogin() {
        navController.navigate(
            R.id.loginFragment,
            null,
            NavOptions.Builder().setPopUpTo(R.id.splashFragment, true).build()
        )
    }

    override fun navigateLoginToMain() {
        navController.navigate(R.id.action_login_to_main)
    }

    override fun navigateLoginToRegister() {
        navController.navigate(R.id.action_login_to_register)
    }

    override fun navigateRegisterToMain() {
        navController.navigate(R.id.action_register_to_main)
    }

    override fun navigateMainToChat(peerUserId: Long, groupId: Long, titleFallback: String) {
        navController.navigate(
            R.id.action_main_to_chat,
            bundleOf(
                "peerUserId" to peerUserId,
                "groupId" to groupId,
                "titleFallback" to titleFallback
            )
        )
    }

    override fun navigateLogoutToLogin() {
        navController.navigate(
            R.id.loginFragment,
            null,
            NavOptions.Builder().setPopUpTo(R.id.mainFragment, true).build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSplashScreen.get() }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        consumeLaunchChatIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchChatIntent(intent)
    }

    private fun consumeLaunchChatIntent(intent: Intent?) {
        if (intent == null) return
        val peer = intent.getLongExtra(ImMessageNotifier.EXTRA_OPEN_PEER_USER_ID, -1L)
        val group = intent.getLongExtra(ImMessageNotifier.EXTRA_OPEN_GROUP_ID, -1L)
        if (peer <= 0L && group <= 0L) return
        val title = intent.getStringExtra(ImMessageNotifier.EXTRA_OPEN_TITLE_FALLBACK).orEmpty()
        services.pendingChatNavigation = PendingChatNavigation(peer, group, title)
    }
}
