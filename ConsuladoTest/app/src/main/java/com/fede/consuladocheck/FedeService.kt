package com.fede.consuladocheck

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.Calendar
import java.util.Date

class FedeService : Service() {

    companion object {
        private const val TAG = "FedeService"
        private const val SHORT_INTERVAL: Long = 30 * 1000 // 30sec in milliseconds
        private const val LONG_INTERVAL: Long = 60 * 60 * 1000 // 30sec in milliseconds
    }
    var sleepInterval: Long = 0;
    var CHANNEL_FRONT_ID :String = "front_channel";
    var CHANNEL_ID :String = "channel_01";
    lateinit var fetchedData: String

    private lateinit var mediaPlayer: MediaPlayer

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            // Do something
            Log.d(TAG, "Performing periodic action every 5 minutes")

            sleepInterval = if (UseShortInterval()) SHORT_INTERVAL else LONG_INTERVAL;

            fetchHtml(MainActivity.ActionToDo.Check)

            // Schedule the next execution
            handler.postDelayed(this, SHORT_INTERVAL)
        }
    }

    fun UseShortInterval() :Boolean
    {
        val date: Date = Calendar.getInstance().time
        val cal = Calendar.getInstance()
        cal.time = date
        val hours = cal.get(Calendar.HOUR_OF_DAY)

        return hours > 7;
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        // Start the initial runnable task by posting through the handler
        handler.post(runnable)
        FrontNotif()
        mediaPlayer = MediaPlayer.create(this, R.raw.metal)
        // Return START_STICKY to keep the service running until explicitly stopped
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")

        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        // Remove any pending callbacks to prevent them from executing after the service is stopped
        handler.removeCallbacks(runnable)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("ForegroundServiceType")
    private fun FrontNotif()
    {
        var builder = NotificationCompat.Builder(this, CHANNEL_FRONT_ID)
            .setSmallIcon(androidx.core.R.drawable.notification_bg_normal_pressed)
            .setContentTitle("Front ")
            .setContentText("Front Service Running")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        startForeground(2, builder.build())
    }

    private fun playSound() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    private fun fetchHtml(wantedAction : MainActivity.ActionToDo) {
        // Launch a coroutine to perform the network operation
        var actionToDo : MainActivity.ActionToDo;
        actionToDo = wantedAction;
        var data = ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://www.cgeonline.com.ar/informacion/apertura-de-citas.html"  // Replace with the URL you want to check
                val doc: Document = Jsoup.connect(url).get()

                // Extract information from the document
                val title = doc.title()
                val elementText = doc.selectFirst("div.some-class")?.text() ?: "Element not found."
                val tdElement = doc.selectFirst("td:contains(Pasaportes)")

                if (tdElement != null) {
                    for (element in tdElement.siblingElements()) {
                        data += element.text() + "\n" // Concatenate text from sibling elements
                    }
                } else {
                    data = "Element not found."
                    actionToDo = MainActivity.ActionToDo.Error
                }
                // Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    BroadcastToApp(data)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                actionToDo = MainActivity.ActionToDo.Error
                withContext(Dispatchers.Main) {
                    BroadcastToApp("Error fetching the web page.")
                }
            }

            if (actionToDo == MainActivity.ActionToDo.Check && fetchedData == "")
            {
                actionToDo = MainActivity.ActionToDo.Fetch;
            }

            when (actionToDo) {
                MainActivity.ActionToDo.Fetch -> fetchedData = data
                MainActivity.ActionToDo.Check -> {
                    if (fetchedData != data)
                    {
                        FreakOut()
                    }
                }
                MainActivity.ActionToDo.Error -> {
                    BroadcastToApp("ERROR!!!")
                }
                else -> { // Note the block
                    BroadcastToApp("something went wrong!!!")
                }
            }
        }
    }

    private fun BroadcastToApp(s:String)
    {
        //textFeedback.text = s
        val intent = Intent("com.example.UPDATE_STATUS_TEXT")
        intent.putExtra("message", s)
        intent.putExtra("interval", sleepInterval/1000/60)
        intent.putExtra("fetchedData", fetchedData)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    private fun FreakOut()
    {
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(androidx.core.R.drawable.notification_icon_background)
            .setContentTitle("CORRE")
            .setContentText("CORRE GUACHO DALE")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@FedeService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                println("WTF!!!!")
                return@with
            }
            // notificationId is a unique int for each notification that you must define.
            notify(1, builder.build())
        }
        playSound()
    }
}
