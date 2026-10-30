package com.akreutz.knitting.web

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.akreutz.knitting.MainActivity
import com.akreutz.knitting.R
import com.akreutz.knitting.data.CounterRepository
import com.akreutz.knitting.data.KnittingDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

/** Keeps the app alive while the web server runs, so a browser on the same network can read and change the counters. */
class WebCounterService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        if (job == null) {
            job = scope.launch {
                val counters = CounterRepository(KnittingDatabase.get(applicationContext).projectDao())
                val server = CounterWebServer(
                    port = WebSettings.PORT,
                    pin = { WebSettings.pin(applicationContext) },
                    page = { page() },
                    state = { WebProtocol.encode(counters.active()) },
                    step = { counters.stepStep(it.stepId, it.delta) },
                )
                try {
                    server.boundPort.invokeOnCompletion { if (it == null) WebStatus.set(WebConnection.Running) }
                    server.run()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Most likely the port is taken; the web menu shows the failure.
                    WebStatus.set(WebConnection.Error)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        // A failure stays visible in the web menu; anything else just means the server is no longer running.
        if (WebStatus.connection.value != WebConnection.Error) WebStatus.set(WebConnection.Off)
        super.onDestroy()
    }

    /** The page in the app's language, which the user may have set apart from the phone's. */
    private fun page(): ByteArray {
        val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
        val language = if (locale.language == "de") "de" else "en"
        val html = assets.open(PAGE).use { it.readBytes().decodeToString() }
        return html.replace("<html lang=\"en\">", "<html lang=\"$language\">").toByteArray()
    }

    private fun startInForeground() {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.web_channel))
                .build(),
        )
        val notification = notification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.web_notification_title))
            .setContentText(getString(R.string.web_notification_text, WebSettings.PORT))
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "web"
        const val NOTIFICATION_ID = 2
        const val PAGE = "web/index.html"
    }
}
