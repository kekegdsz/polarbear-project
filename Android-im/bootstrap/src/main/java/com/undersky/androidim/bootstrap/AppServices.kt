package com.undersky.androidim.bootstrap

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.undersky.androidim.notify.ImMessageNotifier
import com.undersky.business.user.AuthApi
import com.undersky.business.user.AuthRepository
import com.undersky.business.user.AuthTokenHolder
import com.undersky.business.user.ContactStore
import com.undersky.business.user.SessionStore
import com.undersky.business.user.UserDirectoryApi
import com.undersky.business.user.UserDirectoryCacheStore
import com.undersky.business.user.UserDirectoryRepository
import com.undersky.im.core.ImCore
import com.undersky.im.core.api.ImClient
import com.undersky.im.core.local.UnreadCountStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.jvm.Volatile

class AppServices private constructor(
    val application: Application,
    val applicationScope: CoroutineScope,
    val sessionStore: SessionStore,
    val unreadCountStore: UnreadCountStore,
    val contactStore: ContactStore,
    val userDirectoryCacheStore: UserDirectoryCacheStore,
    val authRepository: AuthRepository,
    val userDirectoryRepository: UserDirectoryRepository,
    val imClient: ImClient,
) {

    @Volatile
    var pendingChatNavigation: PendingChatNavigation? = null

    fun startSessionRelay() {
        applicationScope.launch {
            sessionStore.sessionFlow.collect { session ->
                AuthTokenHolder.set(session?.token)
                if (session != null) {
                    imClient.connect(session.userId)
                } else {
                    imClient.disconnect(clearUser = true)
                }
            }
        }
    }

    fun registerClearNotificationsOnForeground() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                ImMessageNotifier.cancelAllForApp(application)
            }
        })
    }

    companion object {
        fun create(application: Application, config: HostConfig): AppServices {
            val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

            val sessionStore = SessionStore(application)
            val unreadCountStore = UnreadCountStore(application)
            val contactStore = ContactStore(application)
            val userDirectoryCacheStore = UserDirectoryCacheStore(application)

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val okHttp = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val token = AuthTokenHolder.get()
                    val req = if (token != null) {
                        chain.request().newBuilder().header("X-Auth-Token", token).build()
                    } else {
                        chain.request()
                    }
                    chain.proceed(req)
                }
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("${config.apiBaseUrl.trimEnd('/')}/api/")
                .client(okHttp)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val authRepository = AuthRepository(retrofit.create(AuthApi::class.java))
            val userDirectoryRepository = UserDirectoryRepository(retrofit.create(UserDirectoryApi::class.java))

            val wsClient = okHttp.newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(25, TimeUnit.SECONDS)
                .build()

            val imClient = ImCore.createClient(
                httpClient = wsClient,
                coroutineScope = applicationScope,
                httpBaseUrl = config.apiBaseUrl,
                webSocketPath = config.imWebSocketPath
            )

            return AppServices(
                application = application,
                applicationScope = applicationScope,
                sessionStore = sessionStore,
                unreadCountStore = unreadCountStore,
                contactStore = contactStore,
                userDirectoryCacheStore = userDirectoryCacheStore,
                authRepository = authRepository,
                userDirectoryRepository = userDirectoryRepository,
                imClient = imClient,
            )
        }
    }
}
