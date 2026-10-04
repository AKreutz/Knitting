package com.akreutz.knitting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.akreutz.knitting.ui.KnittingApp
import com.akreutz.knitting.ui.theme.KnittingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KnittingTheme {
                KnittingApp()
            }
        }
    }
}
