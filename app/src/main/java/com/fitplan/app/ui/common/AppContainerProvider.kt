package com.fitplan.app.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.fitplan.app.FitPlanApplication
import com.fitplan.app.di.AppContainer

@Composable
fun rememberAppContainer(): AppContainer {
    val appContext = LocalContext.current.applicationContext
    return appContext.asFitPlanApplication().appContainer
}

private fun Context.asFitPlanApplication(): FitPlanApplication {
    return this as FitPlanApplication
}

