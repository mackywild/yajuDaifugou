package com.example.daifugo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.daifugo.android.ui.DaifugoApp
import com.example.daifugo.android.ui.theme.DaifugoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaifugoTheme {
                val viewModel: DaifugoViewModel = viewModel()
                DaifugoApp(viewModel)
            }
        }
    }
}
