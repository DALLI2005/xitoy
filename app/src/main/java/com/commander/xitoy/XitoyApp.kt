package com.commander.xitoy

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.commander.xitoy.domain.model.SessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class XitoyApp : Application() {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        Coil.setImageLoader(imageLoader)
    }
}
