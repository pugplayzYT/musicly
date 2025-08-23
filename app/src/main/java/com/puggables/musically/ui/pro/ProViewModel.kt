package com.puggables.musically.ui.pro

import androidx.lifecycle.*
import com.puggables.musically.MusicallyApplication
import com.puggables.musically.data.remote.RetrofitInstance
import kotlinx.coroutines.launch

data class ActivationResult(val success: Boolean, val message: String)

class ProViewModel : ViewModel() {

    private val _activationResult = MutableLiveData<ActivationResult>()
    val activationResult: LiveData<ActivationResult> = _activationResult

    fun activatePro(token: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.activatePro(mapOf("qr_token" to token))
                if (response.isSuccessful) {
                    MusicallyApplication.sessionManager.setUserAsPro()
                    _activationResult.postValue(ActivationResult(true, "Pro Membership Activated!"))
                } else {
                    _activationResult.postValue(ActivationResult(false, "Activation failed: Invalid token."))
                }
            } catch (e: Exception) {
                _activationResult.postValue(ActivationResult(false, "Activation failed: Network error."))
            }
        }
    }
}