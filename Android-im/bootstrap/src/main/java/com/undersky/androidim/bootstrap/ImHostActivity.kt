package com.undersky.androidim.bootstrap

interface ImHostActivity {
    fun endSplashHold()
    fun requestPostNotificationsIfNeeded()
    fun navigateSplashToMain()
    fun navigateSplashToLogin()
    fun navigateLoginToMain()
    fun navigateLoginToRegister()
    fun navigateRegisterToMain()
    fun navigateMainToChat(peerUserId: Long, groupId: Long, titleFallback: String)
    fun navigateLogoutToLogin()
}
