package com.puggables.musically.ui.artist

import androidx.lifecycle.ViewModel
import com.puggables.musically.data.models.Artist
import com.puggables.musically.data.remote.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ArtistProfileVM : ViewModel() {
    fun load(artistId: Int, cb: (Artist?, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = RetrofitInstance.api.getArtist(artistId)
                if (resp.isSuccessful) cb(resp.body(), null)
                else cb(null, "HTTP ${resp.code()}")
            } catch (e: Exception) {
                cb(null, e.message ?: "error")
            }
        }
    }
}
