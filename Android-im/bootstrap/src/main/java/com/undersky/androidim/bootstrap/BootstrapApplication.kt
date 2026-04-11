package com.undersky.androidim.bootstrap

import android.app.Application

abstract class BootstrapApplication : Application() {

    lateinit var services: AppServices
        private set

    protected abstract fun hostConfig(): HostConfig

    override fun onCreate() {
        super.onCreate()
        services = AppServices.create(this, hostConfig())
        services.startSessionRelay()
        services.registerClearNotificationsOnForeground()
    }
}
