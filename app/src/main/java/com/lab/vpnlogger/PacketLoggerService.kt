package com.lab.vpnlogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PacketLoggerService : VpnService() {

    private var vpnFd: ParcelFileDescriptor? = null
    private lateinit var logDir: File
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        logDir = File(getExternalFilesDir(null), "vpn_logged")
        logDir.mkdirs()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = Builder()
            .addAddress("10.200.1.2", 24)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .setSession("LabPacketLogger")
        vpnFd = builder.establish()
        Thread { captureLoop() }.start()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = "vpn_logger_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Packet Logger",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("Lab VPN Active")
            .setContentText("Logging packets to vpn_logged")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }

    private fun captureLoop() {
        running = true
        val tsFormat = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
        try {
            val input = FileInputStream(vpnFd!!.fileDescriptor)
            val buffer = ByteArray(4096)
            while (running) {
                val len = input.read(buffer)
                if (len > 0) {
                    val payload = buffer.copyOf(len)
                    val fname = "pkt_${tsFormat.format(Date())}_${System.nanoTime()}.hex"
                    File(logDir, fname).writeText(
                        payload.joinToString("") { "%02x".format(it) }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        running = false
        vpnFd?.close()
        super.onDestroy()
    }
}
