package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.StudyViewModel
import com.example.ui.StudyViewModelFactory
import com.example.ui.screens.MainAppContainer
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AbhyasApplication
        val factory = StudyViewModelFactory(app, app.repository)
        val viewModel = ViewModelProvider(this, factory)[StudyViewModel::class.java]

        setContent {
            MyApplicationTheme(darkTheme = viewModel.isDarkTheme) {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}
