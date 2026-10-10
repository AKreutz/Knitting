package com.akreutz.knitting

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.akreutz.knitting.ui.KnittingApp
import com.akreutz.knitting.ui.theme.KnittingTheme
import com.akreutz.knitting.watch.WatchSettings

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light-only theme: keep system bar icons dark even when the device is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        // Android may have stopped the service since the last launch; opening the app brings it back.
        if (WatchSettings.isEnabled(this)) WatchSettings.apply(this)
        setContent {
            KnittingTheme {
                KnittingApp()
            }
        }
    }
}
