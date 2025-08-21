package com.puggables.musically

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.puggables.musically.data.models.Song

class MainViewModel : ViewModel() {

    private val _currentPlayingSong = MutableLiveData<Song?>()
    val currentPlayingSong: LiveData<Song?> = _currentPlayingSong

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    var mediaController: Player? = null
        set(value) {
            field?.removeListener(playerListener)
            field = value
            value?.addListener(playerListener)
            _isPlaying.postValue(value?.isPlaying == true)
        }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.postValue(isPlaying)
        }
        override fun onEvents(player: Player, events: Player.Events) {
            _isPlaying.postValue(player.isPlaying)
        }
    }

    private val BASE = "https://cents-mongolia-difficulties-mortgage.trycloudflare.com"

    fun playOrToggleSong(song: Song, isNewSong: Boolean = false) {
        val ctrl = mediaController ?: return
        val stream = (song.streamUrl ?: "$BASE/static/music/${song.filepath}").replace(" ", "%20")
        val artUrl = song.imageUrl ?: "$BASE/static/images/${song.image}"

        val isSame = ctrl.currentMediaItem?.mediaId == stream
        if (isSame && !isNewSong) {
            if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
            return
        }

        val meta = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(Uri.parse(artUrl))
            .build()

        val item = MediaItem.Builder()
            .setMediaId(stream)
            .setUri(stream)
            .setMediaMetadata(meta)
            .build()

        ctrl.setMediaItem(item)
        ctrl.prepare()
        ctrl.play()
        _currentPlayingSong.postValue(song)
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        mediaController?.release()
    }
}
