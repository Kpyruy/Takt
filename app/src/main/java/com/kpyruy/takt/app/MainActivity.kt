package com.kpyruy.takt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kpyruy.takt.core.ui.theme.TaktTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as TaktApplication).dataContainer.studyPlanRepository
        setContent {
            TaktTheme {
                TaktApp(repository)
            }
        }
    }
}
