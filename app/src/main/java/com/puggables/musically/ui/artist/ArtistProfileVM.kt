package com.puggables.musically.ui.artist

import androidx.lifecycle.ViewModel
import com.puggables.musically.data.models.Artist
import com.puggables.musically.data.remote.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistProfileVM : ViewModel() {
    fun load(artistId: Int, cb: (Artist?, String?) -> Unit) {
        // This starts the operation on a background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = RetrofitInstance.api.getArtist(artistId)

                // vvv THIS IS THE FIX vvv
                // Switch back to the main thread before calling the callback
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        cb(resp.body(), null)
                    } else {
                        cb(null, "HTTP ${resp.code()}")
                    }
                }
            } catch (e: Exception) {
                // Switch back to the main thread for error case as well
                withContext(Dispatchers.Main) {
                    cb(null, e.message ?: "error")
                }
            }
        }
    }
}