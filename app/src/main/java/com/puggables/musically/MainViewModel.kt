package com.puggables.musically

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import com.puggables.musically.data.models.Song
import java.io.File

class MainViewModel : ViewModel() {

    private val _currentPlayingSong = MutableLiveData<Song?>()
    val currentPlayingSong: LiveData<Song?> = _currentPlayingSong

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _playbackSpeed = MutableLiveData(1.0f)
    val playbackSpeed: LiveData<Float> = _playbackSpeed

    var mediaController: Player? = null
        set(value) {
            field?.removeListener(playerListener)
            field = value
            value?.addListener(playerListener)
            _isPlaying.postValue(value?.isPlaying == true)
            // When the player is ready, set the default speed
            applyDefaultSpeed()
        }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.postValue(isPlaying)
        }
        override fun onEvents(player: Player, events: Player.Events) {
            _isPlaying.postValue(player.isPlaying)
        }
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            super.onPlaybackParametersChanged(playbackParameters)
            _playbackSpeed.postValue(playbackParameters.speed)
        }
    }

    fun playOrToggleSong(song: Song, isNewSong: Boolean = false) {
        val ctrl = mediaController ?: return

        // FIX: Check if the song is local or remote
        val isLocal = song.streamUrl?.startsWith("/") == true || song.streamUrl?.startsWith("file://") == true
        val streamUri: Uri
        val artUri: Uri

        if (isLocal) {
            // It's a downloaded song, use file paths
            streamUri = Uri.fromFile(File(song.streamUrl!!))
            artUri = if (song.imageUrl != null) Uri.fromFile(File(song.imageUrl)) else Uri.EMPTY
        } else {
            // It's a streaming song, use web URLs
            val baseUrl = com.puggables.musically.data.remote.RetrofitInstance.currentBaseUrl
            streamUri = Uri.parse((song.streamUrl ?: "${baseUrl}static/music/${song.filepath}").replace(" ", "%20"))
            artUri = Uri.parse(song.imageUrl ?: "${baseUrl}static/images/${song.image}")
        }

        val isSame = ctrl.currentMediaItem?.mediaId == streamUri.toString()
        if (isSame && !isNewSong) {
            if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
            return
        }

        // When a new song starts, apply the default speed
        if (isNewSong) {
            applyDefaultSpeed()
        }

        val meta = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(artUri)
            .build()

        val item = MediaItem.Builder()
            .setMediaId(streamUri.toString())
            .setUri(streamUri)
            .setMediaMetadata(meta)
            .build()

        ctrl.setMediaItem(item)
        ctrl.prepare()
        ctrl.play()
        _currentPlayingSong.postValue(song)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clampedSpeed = speed.coerceIn(1.0f, 3.5f)
        mediaController?.let { player ->
            player.setPlaybackSpeed(clampedSpeed)
        }
    }

    // New private function to apply the saved speed
    private fun applyDefaultSpeed() {
        val defaultSpeed = MusicallyApplication.sessionManager.getDefaultSpeed()
        setPlaybackSpeed(defaultSpeed)
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        mediaController?.release()
    }
}
