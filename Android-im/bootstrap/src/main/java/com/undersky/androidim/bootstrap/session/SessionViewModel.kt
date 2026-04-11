package com.undersky.androidim.bootstrap.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.business.user.UserSession
import kotlinx.coroutines.flow.map

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val services = (application as BootstrapApplication).services

    val session: LiveData<UserSession?> = services.sessionStore.sessionFlow
        .map { it }
        .asLiveData(timeoutInMs = 5000L)
}
