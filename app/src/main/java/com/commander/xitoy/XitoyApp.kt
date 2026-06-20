package com.commander.xitoy

import android.app.Application
import com.commander.xitoy.domain.model.SessionManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class XitoyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
