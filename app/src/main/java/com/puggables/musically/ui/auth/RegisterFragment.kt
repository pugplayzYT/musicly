package com.puggables.musically.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.puggables.musically.R
import com.puggables.musically.databinding.FragmentRegisterBinding
import com.puggables.musically.ui.common.goHomeClearingBackStack

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        binding.registerButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                viewModel.register(username, password)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.goToLoginText.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_settingsFragment)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.authResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                // ✅ global action to home, clear backstack
                findNavController().goHomeClearingBackStack()
            } else {
                Toast.makeText(requireContext(), "Registration Failed", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
