package com.musiclib.data

import android.content.Context

/** Manual DI — single source of truth for app-wide singletons. */
class AppContainer(context: Context) {
    val settings: SettingsRepository = SettingsRepository(context.applicationContext)
    val api: MusicApi = MusicApi(settings)
}
