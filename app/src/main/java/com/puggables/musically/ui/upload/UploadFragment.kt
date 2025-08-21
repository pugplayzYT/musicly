package com.puggables.musically.ui.upload

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.puggables.musically.R
import com.puggables.musically.databinding.FragmentUploadBinding
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class UploadFragment : Fragment(R.layout.fragment_upload) {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private var selectedAudio: Uri? = null
    private var selectedImage: Uri? = null

    private val pickAudio = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedAudio = uri
            val name = FileUtils.getDisplayName(requireContext(), uri) ?: "audio_selected"
            binding.audioNameText.text = "Audio: $name"
        }
    }

    private val pickImage = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImage = uri
            val name = FileUtils.getDisplayName(requireContext(), uri) ?: "image_selected"
            binding.imageNameText.text = "Image: $name"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUploadBinding.bind(view)

        binding.pickAudioButton.setOnClickListener { pickAudio.launch("audio/*") }
        binding.pickImageButton.setOnClickListener { pickImage.launch("image/*") }
        binding.uploadButton.setOnClickListener { doUpload() }
    }

    private fun setUploading(isUploading: Boolean) {
        binding.progressBar.isVisible = isUploading
        binding.uploadButton.isEnabled = !isUploading
        binding.pickAudioButton.isEnabled = !isUploading
        binding.pickImageButton.isEnabled = !isUploading
        binding.titleEditText.isEnabled = !isUploading
        binding.albumEditText.isEnabled = !isUploading
        binding.genresEditText.isEnabled = !isUploading // can hide/remove if not used anymore
    }

    private fun doUpload() {
        val title = binding.titleEditText.text?.toString()?.trim().orEmpty()
        val albumInput = binding.albumEditText.text?.toString()?.trim()
        val album = if (albumInput.isNullOrEmpty()) "Single" else albumInput
        val audioUri = selectedAudio

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Title is required.", Toast.LENGTH_SHORT).show()
            return
        }
        if (audioUri == null) {
            Toast.makeText(requireContext(), "Pick an audio file.", Toast.LENGTH_SHORT).show()
            return
        }

        setUploading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    // copy content:// to temp files
                    val audioFile: File = FileUtils.copyUriToTempFile(
                        requireContext(),
                        audioUri,
                        suffix = FileUtils.suffixFromMime(FileUtils.getMime(requireContext(), audioUri), ".mp3")
                    )
                    val audioMime = FileUtils.getMime(requireContext(), audioUri) ?: "audio/mpeg"
                    val audioBody = audioFile.asRequestBody(audioMime.toMediaTypeOrNull())
                    val audioPart = MultipartBody.Part.createFormData("audio", audioFile.name, audioBody)

                    val imagePart: MultipartBody.Part? = selectedImage?.let { imgUri ->
                        val imageFile: File = FileUtils.copyUriToTempFile(
                            requireContext(),
                            imgUri,
                            suffix = FileUtils.suffixFromMime(FileUtils.getMime(requireContext(), imgUri), ".png")
                        )
                        val imgMime = FileUtils.getMime(requireContext(), imgUri) ?: "image/png"
                        val imgBody = imageFile.asRequestBody(imgMime.toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("image", imageFile.name, imgBody)
                    }

                    val titleRB: RequestBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                    val albumRB: RequestBody = album.toRequestBody("text/plain".toMediaTypeOrNull())

                    val response = RetrofitInstance.api.uploadSong(
                        title = titleRB,
                        album = albumRB,
                        audio = audioPart,
                        image = imagePart
                    )

                    response.isSuccessful
                } catch (_: Exception) {
                    false
                }
            }

            setUploading(false)

            if (success) {
                Toast.makeText(requireContext(), "Uploaded 🎵", Toast.LENGTH_SHORT).show()
                binding.titleEditText.text?.clear()
                binding.albumEditText.text?.clear()
                binding.genresEditText.text?.clear()
                selectedAudio = null
                selectedImage = null
                binding.audioNameText.text = "Audio: none"
                binding.imageNameText.text = "Image: none"
            } else {
                Toast.makeText(requireContext(), "Upload failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
