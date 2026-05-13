package com.musiclib

import android.app.Application
import com.musiclib.data.AppContainer

class MusicLibApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
