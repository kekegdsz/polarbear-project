package com.undersky.androidim

import android.app.Application
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.undersky.androidim.data.AuthApi
import com.undersky.androidim.data.AuthRepository
import com.undersky.androidim.data.AuthTokenHolder
import com.undersky.androidim.data.ContactStore
import com.undersky.androidim.data.ImSocketManager
import com.undersky.androidim.data.SessionStore
import com.undersky.androidim.data.UserDirectoryApi
import com.undersky.androidim.data.UserDirectoryCacheStore
import com.undersky.androidim.data.UserDirectoryRepository
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
    lateinit var imSocket: ImSocketManager
        private set

    override fun onCreate() {
        super.onCreate()
        sessionStore = SessionStore(this)
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

        imSocket = ImSocketManager(
            client = wsClient,
            scope = applicationScope,
            wsUrl = ImSocketManager.buildWsUrl(BuildConfig.API_BASE_URL, BuildConfig.IM_WS_PATH)
        )
    }
}
