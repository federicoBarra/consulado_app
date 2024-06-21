package com.fede.consuladocheck

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.text.SimpleDateFormat
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    var CHANNEL_FRONT_ID :String = "front_channel"
    var CHANNEL_ID :String = "channel_01"

    enum class ActionToDo {
        Fetch, Check, Error
    }

    private lateinit var textLastText: TextView
    private lateinit var textFeedback: TextView

    private lateinit var  buttonOn: Button
    private lateinit var  buttonOff: Button
    private lateinit var  buttonTestNow: Button

    private lateinit var localBroadcastManager: LocalBroadcastManager
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("message")
            val interval = intent.getStringExtra("interval")
            val fetchedData = intent.getStringExtra("fetchedData")

            val date = Calendar.getInstance().time
            val formatter = SimpleDateFormat.getDateTimeInstance() //or use getDateInstance()
            val formatedDate = formatter.format(date)
            textLastText.text = fetchedData

            Feedback("$formatedDate \n $message \n Service is on. Refresh every $interval minutes.")
        }
    }
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        InitRefs()
        CreateNotificationChannel()
        CreateFrontNotificationChannel()
        AskPermissions()

        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        // Register the receiver
        val filter = IntentFilter("com.example.UPDATE_STATUS_TEXT")
        localBroadcastManager.registerReceiver(receiver, filter)
    }
    private fun TurnOn()
    {
        Feedback("TurnOn")
        val serviceIntent = Intent(this, FedeService::class.java)
        this.startService(serviceIntent)
    }
    private fun TurnOff()
    {
        Feedback("TurnOff")
        val serviceIntent = Intent(this, FedeService::class.java)
        this.stopService(serviceIntent)
    }
    private fun Test()
    {
        Feedback("Test, nothing done")
    }
    private fun Feedback(s:String)
    {
        textFeedback.text = s
    }

    private fun CreateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            println("CreateNotificationChannel")
            val channel = NotificationChannel(CHANNEL_ID, "CORRE", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Channel desc"
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun CreateFrontNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            println("CreateFrontNotificationChannel")
            val channel = NotificationChannel(CHANNEL_FRONT_ID, "FRONT", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "FRONT Channel desc"
            // Register the channel with the system.
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun AskPermissions()
    {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                FULLSCREEN_MODE_REQUEST_ENTER
            )
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.FOREGROUND_SERVICE),
                FULLSCREEN_MODE_REQUEST_ENTER
            )
        }
    }

    private fun InitRefs()
    {
        textLastText = findViewById(R.id.last_text)
        textFeedback = findViewById(R.id.text_status)

        buttonOn = findViewById(R.id.button_turn_on)
        buttonOn.setOnClickListener { TurnOn()}
        buttonOff = findViewById(R.id.button_turn_off)
        buttonOff.setOnClickListener { TurnOff()}
        buttonTestNow = findViewById(R.id.button_test_now)
        buttonTestNow.setOnClickListener { Test()}
    }
}

//var path: String = "https://www.cgeonline.com.ar/informacion/apertura-de-citas.html"
//lateinit var fetchedData: String



//    private fun FreakOut()
//    {
//        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(androidx.core.R.drawable.notification_icon_background)
//            .setContentTitle("CORRE")
//            .setContentText("CORRE GUACHO DALE")
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//
//        with(NotificationManagerCompat.from(this)) {
//            if (ActivityCompat.checkSelfPermission(
//                    this@MainActivity,
//                    Manifest.permission.POST_NOTIFICATIONS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                println("WTF!!!!")
//                return@with
//            }
//            // notificationId is a unique int for each notification that you must define.
//            notify(1, builder.build())
//        }
//    }

//          private fun fetchHtml(wantedAction : ActionToDo) {
//        // Launch a coroutine to perform the network operation
//        var actionToDo : ActionToDo
//        actionToDo = wantedAction
//        var data = ""
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val url = "https://www.cgeonline.com.ar/informacion/apertura-de-citas.html"  // Replace with the URL you want to check
//                val doc: Document = Jsoup.connect(url).get()
//
//                // Extract information from the document
////                val title = doc.title()
////                val elementText = doc.selectFirst("div.some-class")?.text() ?: "Element not found."
//                val tdElement = doc.selectFirst("td:contains(Pasaportes)")
//
//                if (tdElement != null) {
//                    for (element in tdElement.siblingElements()) {
//                        data += element.text() + "\n" // Concatenate text from sibling elements
//                    }
//                } else {
//                    data = "Element not found."
//                    actionToDo = ActionToDo.Error
//                }
//                // Update the UI on the main thread
//                withContext(Dispatchers.Main) {
//                    Feedback(data)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                actionToDo = ActionToDo.Error
//                withContext(Dispatchers.Main) {
//                    Feedback("Error fetching the web page.")
//                }
//            }
//
//            if (actionToDo == ActionToDo.Check && fetchedData == "")
//            {
//                actionToDo = ActionToDo.Fetch
//            }
//
//            when (actionToDo) {
//                ActionToDo.Fetch -> fetchedData = data
//                ActionToDo.Check -> {
//                    if (fetchedData != data)
//                    {
//                        Feedback("GO CHECK")
//                    }
//                }
//                ActionToDo.Error -> {
//                    Feedback("ERROR!!!")
//                }
//            }
//        }
//    }