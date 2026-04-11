package com.saithanyam.wallpaper.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saithanyam.wallpaper.ui.screen.SetupScreen
import com.saithanyam.wallpaper.ui.theme.SaithanyamTheme
import com.saithanyam.wallpaper.viewmodel.SetupViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaithanyamTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    val viewModel: SetupViewModel = viewModel()
                    SetupScreen(viewModel = viewModel)
                }
            }
        }
    }
}
