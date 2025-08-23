package com.puggables.musically.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puggables.musically.MusicallyApplication
import com.puggables.musically.data.remote.RetrofitInstance
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _authResult = MutableLiveData<Boolean>()
    val authResult: LiveData<Boolean> = _authResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(username: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.login(mapOf("username" to username, "password" to password))
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    // THIS IS THE FIX: We now pass the user's 'is_pro' status
                    MusicallyApplication.sessionManager.saveAuth(user.user_id, user.username, user.token, user.is_pro)
                    _authResult.postValue(true)
                } else {
                    _authResult.postValue(false)
                }
            } catch (_: Exception) {
                _authResult.postValue(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(username: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.register(mapOf("username" to username, "password" to password))
                if (response.isSuccessful) {
                    // auto-login
                    login(username, password)
                } else {
                    _authResult.postValue(false)
                }
            } catch (_: Exception) {
                _authResult.postValue(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}