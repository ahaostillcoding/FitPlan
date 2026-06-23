package com.fitplan.app

import android.app.Application
import com.fitplan.app.di.AppContainer

class FitPlanApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}

