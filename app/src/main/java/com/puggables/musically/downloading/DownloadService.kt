package com.puggables.musically.downloading

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.puggables.musically.R
import com.puggables.musically.data.local.db.AppDatabase
import com.puggables.musically.data.local.db.DownloadedSong
import kotlinx.coroutines.*
import java.io.File
import java.net.URL

class DownloadService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var notificationManager: NotificationManager
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val songId = intent?.getIntExtra("songId", -1) ?: -1
        val title = intent?.getStringExtra("title") ?: "Unknown"
        val artist = intent?.getStringExtra("artist") ?: "Unknown"
        val album = intent?.getStringExtra("album") ?: "Single"
        val duration = intent?.getFloatExtra("duration", 0f) ?: 0f
        val audioUrl = intent?.getStringExtra("audioUrl")
        val imageUrl = intent?.getStringExtra("imageUrl")
        val downloadImages = intent?.getBooleanExtra("downloadImages", false) ?: false

        if (audioUrl != null) {
            scope.launch {
                downloadSong(songId, title, artist, album, duration, audioUrl, imageUrl, downloadImages, startId)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun downloadSong(
        songId: Int, title: String, artist: String, album: String, duration: Float,
        audioUrl: String, imageUrl: String?, downloadImages: Boolean, notificationId: Int
    ) {
        val notification = NotificationCompat.Builder(this, "download_channel")
            .setContentTitle("Downloading: $title")
            .setContentText("Download in progress...")
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(true)
            .setProgress(100, 0, true)

        startForeground(notificationId, notification.build())

        try {
            // Download audio
            val audioFile = downloadFile(audioUrl, ".mp3")
            var imageFile: File? = null

            // Download image if enabled and URL exists
            if (downloadImages && imageUrl != null) {
                imageFile = downloadFile(imageUrl, ".jpg")
            }

            val downloadedSong = DownloadedSong(
                id = songId,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                remoteImageUrl = imageUrl ?: "",
                localAudioPath = audioFile.absolutePath,
                localImagePath = imageFile?.absolutePath
            )
            db.downloadedSongDao().insert(downloadedSong)

            notificationManager.notify(
                notificationId,
                notification.setContentText("Download complete!").setOngoing(false).setProgress(0, 0, false).build()
            )

        } catch (e: Exception) {
            notificationManager.notify(
                notificationId,
                notification.setContentText("Download failed.").setOngoing(false).setProgress(0, 0, false).build()
            )
        } finally {
            stopForeground(false)
        }
    }

    private fun downloadFile(url: String, suffix: String): File {
        val connection = URL(url).openConnection()
        connection.connect()
        val inputStream = connection.getInputStream()
        val file = File(getExternalFilesDir(null), "${System.currentTimeMillis()}$suffix")
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        return file
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "download_channel",
            "Song Downloads",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}