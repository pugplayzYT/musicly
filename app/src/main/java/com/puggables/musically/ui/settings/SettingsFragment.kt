package com.puggables.musically.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import net.glxn.qrgen.android.QRCode
import com.puggables.musically.R
import com.puggables.musically.data.local.SessionManager
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager

    private val speedOptions = arrayOf("1.0x", "1.25x", "1.5x", "2.0x", "3.5x")
    private val speedValues = floatArrayOf(1.0f, 1.25f, 1.5f, 2.0f, 3.5f)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        sessionManager = SessionManager(requireContext())

        // Server URL Logic
        binding.serverUrlEditText.setText(sessionManager.getBaseUrl())
        binding.saveUrlButton.setOnClickListener {
            var newUrl = binding.serverUrlEditText.text.toString().trim()
            if (newUrl.isNotEmpty() && (newUrl.startsWith("http://") || newUrl.startsWith("https://"))) {
                if (!newUrl.endsWith("/")) {
                    newUrl += "/"
                }
                sessionManager.saveBaseUrl(newUrl)
                RetrofitInstance.reinitializeApi()
                Toast.makeText(requireContext(), "App URL saved!", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch {
                    try {
                        val serverUrl = newUrl.removeSuffix("/")
                        val response = RetrofitInstance.api.updateServerUrl(mapOf("url" to serverUrl))
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Server URL updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Failed to update server URL", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a valid URL", Toast.LENGTH_LONG).show()
            }
        }

        setupDefaultSpeedSpinner()
        setupProSection()
    }

    private fun setupDefaultSpeedSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, speedOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.defaultSpeedSpinner.adapter = adapter

        val currentSpeed = sessionManager.getDefaultSpeed()
        val currentSpeedIndex = speedValues.indexOfFirst { it == currentSpeed }.coerceAtLeast(0)
        binding.defaultSpeedSpinner.setSelection(currentSpeedIndex)

        binding.defaultSpeedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedSpeed = speedValues[position]
                sessionManager.saveDefaultSpeed(selectedSpeed)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupProSection() {
        // Admin build flag (change this to false for normal user builds)
        val isAdminBuild = true
        binding.generateQrButton.visibility = if (isAdminBuild) View.VISIBLE else View.GONE

        if (sessionManager.isPro()) {
            binding.proStatusText.text = "Status: Musically Pro"
            binding.becomeProButton.visibility = View.GONE
        } else {
            binding.proStatusText.text = "Status: Standard"
            binding.becomeProButton.visibility = View.VISIBLE
        }

        binding.becomeProButton.setOnClickListener {
            findNavController().navigate(R.id.action_global_qrScannerFragment)
        }

        binding.generateQrButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val response = RetrofitInstance.api.generateProQrToken()
                    if (response.isSuccessful) {
                        response.body()?.get("qr_token")?.let { token ->
                            showQrCodeDialog(token)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error generating token.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showQrCodeDialog(token: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_generate_qr, null)
        val qrCodeImageView = dialogView.findViewById<ImageView>(R.id.qrCodeImageView)
        // This call will now work because of the corrected import
        val bitmap = QRCode.from(token).withSize(1000, 1000).bitmap()
        qrCodeImageView.setImageBitmap(bitmap)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}