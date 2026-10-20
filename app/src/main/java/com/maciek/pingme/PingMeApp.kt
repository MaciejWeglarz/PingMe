package com.maciek.pingme

import android.app.Application
import com.maciek.pingme.data.reminder.Notifier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PingMeApp : Application() {

    @Inject lateinit var notifier: Notifier

    override fun onCreate() {
        super.onCreate()
        // Kanały muszą istnieć, zanim pokażemy pierwsze powiadomienie.
        notifier.createChannels()
    }
}
