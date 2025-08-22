package com.puggables.musically.ui.upload

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puggables.musically.data.models.Album
import com.puggables.musically.data.remote.RetrofitInstance
import kotlinx.coroutines.launch

class UploadViewModel : ViewModel() {

    private val _albums = MutableLiveData<List<Album>>()
    val albums: LiveData<List<Album>> = _albums

    fun fetchMyAlbums() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getMyAlbums()
                if (response.isSuccessful) {
                    _albums.postValue(response.body())
                }
            } catch (e: Exception) {
                // In a real app, you'd post an error to the UI
                _albums.postValue(emptyList()) // Post an empty list on error
            }
        }
    }
}