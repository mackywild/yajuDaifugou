package com.example.daifugo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.daifugo.android.playgames.PlayGamesAccountManager
import com.example.daifugo.android.ui.DaifugoApp
import com.google.android.gms.games.PlayGamesSdk
import com.example.daifugo.android.ui.theme.DaifugoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PlayGamesAccountManager.isConfigured(this)) {
            PlayGamesSdk.initialize(this)
        }
        enableEdgeToEdge()
        setContent {
            DaifugoTheme {
                val viewModel: DaifugoViewModel = viewModel()
                DaifugoApp(viewModel)
            }
        }
    }
}
