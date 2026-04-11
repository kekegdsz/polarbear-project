package com.undersky.androidim

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.undersky.androidim.data.UnreadCountStore
import com.undersky.androidim.notify.ImMessageNotifier
import com.undersky.androidim.notify.PendingChatNavigation
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ImApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var sessionStore: SessionStore
        private set
    lateinit var contactStore: ContactStore
        private set
    lateinit var userDirectoryCacheStore: UserDirectoryCacheStore
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var userDirectoryRepository: UserDirectoryRepository
        private set
    lateinit var imClient: ImClient
        private set

    lateinit var unreadCountStore: UnreadCountStore
        private set

    @Volatile
    var pendingChatNavigation: PendingChatNavigation? = null

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
        unreadCountStore = UnreadCountStore(this)
        contactStore = ContactStore(this)
        userDirectoryCacheStore = UserDirectoryCacheStore(this)

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
            .baseUrl("${BuildConfig.API_BASE_URL.trimEnd('/')}/api/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        authRepository = AuthRepository(retrofit.create(AuthApi::class.java))
        userDirectoryRepository = UserDirectoryRepository(retrofit.create(UserDirectoryApi::class.java))

        val wsClient = okHttp.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(25, TimeUnit.SECONDS)
            .build()

        imClient = ImCore.createClient(
            httpClient = wsClient,
            coroutineScope = applicationScope,
            httpBaseUrl = BuildConfig.API_BASE_URL,
            webSocketPath = BuildConfig.IM_WS_PATH
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                ImMessageNotifier.cancelAllForApp(this@ImApp)
            }
        })
    }
}
