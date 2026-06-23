package com.fitplan.app.di

import android.content.Context
import com.fitplan.app.data.local.database.FitPlanDatabase

class AppContainer(
    appContext: Context
) {
    val database: FitPlanDatabase = FitPlanDatabase.create(appContext)
}
