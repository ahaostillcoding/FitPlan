package com.fitplan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fitplan.app.navigation.FitPlanNavHost
import com.fitplan.app.ui.theme.FitPlanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitPlanTheme {
                FitPlanNavHost()
            }
        }
    }
}
