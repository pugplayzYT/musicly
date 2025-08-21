package com.puggables.musically.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puggables.musically.data.models.Song
import com.puggables.musically.data.remote.RetrofitInstance // <-- This was missing
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> = _songs

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        fetchSongs()
    }

    private fun fetchSongs() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getAllSongs()
                if (response.isSuccessful) {
                    _songs.postValue(response.body())
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}