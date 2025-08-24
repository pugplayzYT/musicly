package com.puggables.musically.downloading

import com.puggables.musically.data.local.db.DownloadedSongDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

// A sealed class to represent the different states of a download.
sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Int) : DownloadStatus()
    object Downloaded : DownloadStatus()
}

// This is a singleton object that acts as a central hub for tracking all downloads.
object DownloadTracker {

    // A thread-safe map to hold the state of each download.
    // The key is the song ID, and the value is a StateFlow that emits DownloadStatus updates.
    private val downloadStatusFlows = ConcurrentHashMap<Int, MutableStateFlow<DownloadStatus>>()
    private var songDao: DownloadedSongDao? = null

    // This needs to be called once when the app starts.
    fun initialize(dao: DownloadedSongDao) {
        songDao = dao
    }

    // Gets the status flow for a specific song, creating it if it doesn't exist.
    fun getDownloadStatusFlow(songId: Int): StateFlow<DownloadStatus> {
        return downloadStatusFlows.getOrPut(songId) {
            // When a flow is first requested, we check the database to see if the song
            // is already downloaded. This makes the status persist across app restarts.
            val initialStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
            CoroutineScope(Dispatchers.IO).launch {
                if (songDao?.getById(songId) != null) {
                    initialStatus.value = DownloadStatus.Downloaded
                }
            }
            initialStatus
        }
    }

    // Called by the DownloadService to update the progress of a download.
    fun updateStatus(songId: Int, status: DownloadStatus) {
        // We ensure the flow exists and then update its value.
        val flow = getDownloadStatusFlow(songId) as? MutableStateFlow
        flow?.value = status
    }
}
