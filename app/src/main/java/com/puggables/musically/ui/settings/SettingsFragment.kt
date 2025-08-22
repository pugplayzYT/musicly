package com.puggables.musically.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.puggables.musically.R
import com.puggables.musically.data.local.SessionManager
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        sessionManager = SessionManager(requireContext())

        // Load the currently saved URL into the text box
        binding.serverUrlEditText.setText(sessionManager.getBaseUrl())

        binding.saveUrlButton.setOnClickListener {
            var newUrl = binding.serverUrlEditText.text.toString().trim()
            if (newUrl.isNotEmpty() && (newUrl.startsWith("http://") || newUrl.startsWith("https://"))) {
                // Ensure the URL ends with a slash, which Retrofit requires
                if (!newUrl.endsWith("/")) {
                    newUrl += "/"
                }
                sessionManager.saveBaseUrl(newUrl)
                // This is the key step: tell Retrofit to use the new URL right away
                RetrofitInstance.reinitializeApi()
                // Fixed the typo here: LENGT_SHORT -> LENGTH_SHORT
                Toast.makeText(requireContext(), "Server URL saved!", Toast.LENGTH_SHORT).show()
            } else {
                // Fixed the typo here: LENGT_LONG -> LENGTH_LONG
                Toast.makeText(requireContext(), "Please enter a valid URL (http:// or https://)", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
