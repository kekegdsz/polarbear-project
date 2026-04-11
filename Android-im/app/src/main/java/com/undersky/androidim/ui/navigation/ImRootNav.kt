package com.undersky.androidim.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.undersky.androidim.ImApp
import com.undersky.androidim.data.UserSession
import com.undersky.androidim.ui.screens.auth.LoginScreen
import com.undersky.androidim.ui.screens.auth.RegisterScreen
import com.undersky.androidim.ui.screens.chat.ChatScreen
import com.undersky.androidim.ui.screens.main.MainTabsScreen

@Composable
fun ImRootNav(session: UserSession?, app: ImApp) {
    val navController = rememberNavController()

    LaunchedEffect(session?.userId) {
        if (session != null) {
            navController.navigate("main") {
                popUpTo("login") { inclusive = true }
                launchSingleTop = true
            }
        } else {
            val route = navController.currentBackStackEntry?.destination?.route
            if (route != null && route != "login" && route != "register") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                app = app,
                onRegister = { navController.navigate("register") },
                onLoggedIn = { }
            )
        }
        composable("register") {
            RegisterScreen(
                app = app,
                onBack = { navController.popBackStack() },
                onRegistered = { }
            )
        }
        composable("main") {
            val s = session ?: return@composable
            MainTabsScreen(
                app = app,
                session = s,
                onOpenChatP2P = { peer -> navController.navigate("chat/p2p/$peer") },
                onOpenChatGroup = { gid -> navController.navigate("chat/group/$gid") }
            )
        }
        composable(
            route = "chat/p2p/{peerUserId}",
            arguments = listOf(navArgument("peerUserId") { type = NavType.LongType })
        ) { entry ->
            val peer = entry.arguments?.getLong("peerUserId") ?: return@composable
            val s = session ?: return@composable
            ChatScreen(
                app = app,
                session = s,
                peerUserId = peer,
                groupId = null,
                titleFallback = "用户 $peer",
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "chat/group/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.LongType })
        ) { entry ->
            val gid = entry.arguments?.getLong("groupId") ?: return@composable
            val s = session ?: return@composable
            ChatScreen(
                app = app,
                session = s,
                peerUserId = null,
                groupId = gid,
                titleFallback = "群聊 $gid",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
