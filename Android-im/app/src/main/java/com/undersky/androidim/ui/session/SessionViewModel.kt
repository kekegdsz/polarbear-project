package com.undersky.androidim.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.undersky.androidim.ImApp
import com.undersky.androidim.data.UserSession
import kotlinx.coroutines.flow.map

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ImApp

    val session: LiveData<UserSession?> = app.sessionStore.sessionFlow
        .map { it }
        .asLiveData(timeoutInMs = 5000L)
}
