package com.akreutz.knitting.watch

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.akreutz.knitting.MainActivity
import com.akreutz.knitting.R
import com.akreutz.knitting.data.CounterRepository
import com.akreutz.knitting.data.KnittingDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Keeps the app alive while the watch controls the counters, so taps on the watch reach the database. */
class WatchCounterService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground(WatchStatus.connection.value)
        if (job == null) {
            job = scope.launch {
                launch {
                    WatchStatus.connection.collect { connection ->
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(connection))
                        if (connection.isFailure) stopSelf()
                    }
                }
                val counters = CounterRepository(KnittingDatabase.get(applicationContext).projectDao())
                WatchCounterController(counters.current, counters::stepCurrent, GarminLink(applicationContext)).run()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        // A failure stays visible in the watch menu; anything else just means the link is no longer running.
        if (!WatchStatus.connection.value.isFailure) WatchStatus.set(WatchConnection.Off)
        super.onDestroy()
    }

    private fun startInForeground(connection: WatchConnection) {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.watch_channel))
                .build(),
        )
        val notification = notification(connection)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notification(connection: WatchConnection): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.watch_notification_title))
            .setContentText(getString(connection.labelRes()))
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "watch"
        const val NOTIFICATION_ID = 1
    }
}

fun WatchConnection.labelRes(): Int = when (this) {
    WatchConnection.Off -> R.string.watch_status_off
    WatchConnection.Connecting -> R.string.watch_status_connecting
    WatchConnection.Connected -> R.string.watch_status_connected
    WatchConnection.WatchOffline -> R.string.watch_status_offline
    WatchConnection.NoWatch -> R.string.watch_status_no_watch
    WatchConnection.GarminConnectMissing -> R.string.watch_status_garmin_missing
    WatchConnection.GarminConnectOutdated -> R.string.watch_status_garmin_outdated
    WatchConnection.Error -> R.string.watch_status_error
}
