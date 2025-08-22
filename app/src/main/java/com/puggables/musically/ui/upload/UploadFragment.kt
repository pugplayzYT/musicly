package com.puggables.musically.ui.upload

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.puggables.musically.R
import com.puggables.musically.data.models.Album
import com.puggables.musically.databinding.FragmentUploadBinding
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class UploadFragment : Fragment(R.layout.fragment_upload) {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UploadViewModel by viewModels()

    private var selectedAudio: Uri? = null
    private var selectedImage: Uri? = null
    private var newAlbumCover: Uri? = null

    // This launcher is for the song's cover art
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImage = uri
            binding.imageNameText.text = "Image: ${FileUtils.getDisplayName(requireContext(), uri)}"
        }
    }

    // This launcher is for the audio file
    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedAudio = uri
            binding.audioNameText.text = "Audio: ${FileUtils.getDisplayName(requireContext(), uri)}"
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUploadBinding.bind(view)

        viewModel.fetchMyAlbums()
        observeViewModel()

        binding.pickAudioButton.setOnClickListener { pickAudio.launch("audio/*") }
        binding.pickImageButton.setOnClickListener { pickImage.launch("image/*") }
        binding.uploadButton.setOnClickListener { doUpload() }
        binding.createNewAlbumButton.setOnClickListener { showCreateAlbumDialog() }
    }

    private fun observeViewModel() {
        viewModel.albums.observe(viewLifecycleOwner) { albums ->
            val albumList = mutableListOf(Album(-1, "Single", -1, null, null))
            albumList.addAll(albums)

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, albumList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.albumSpinner.adapter = adapter
        }
    }

    private fun showCreateAlbumDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_album, null)
        val albumTitleEditText = dialogView.findViewById<EditText>(R.id.albumTitleEditText)
        val albumCoverPreview = dialogView.findViewById<ImageView>(R.id.albumCoverPreview)

        val pickAlbumCoverLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                newAlbumCover = uri
                albumCoverPreview.load(uri) // Update the preview
            }
        }

        albumCoverPreview.setOnClickListener {
            pickAlbumCoverLauncher.launch("image/*")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Create New Album")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val title = albumTitleEditText.text.toString().trim()
                if (title.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val titleRB = title.toRequestBody("text/plain".toMediaTypeOrNull())

                        val imagePart: MultipartBody.Part? = newAlbumCover?.let { uri ->
                            val imageFile = FileUtils.copyUriToTempFile(requireContext(), uri, ".png")
                            val imageMime = FileUtils.getMime(requireContext(), uri) ?: "image/png"
                            MultipartBody.Part.createFormData("cover_image", imageFile.name, imageFile.asRequestBody(imageMime.toMediaTypeOrNull()))
                        }

                        val response = RetrofitInstance.api.createAlbum(titleRB, imagePart)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(requireContext(), "Album created", Toast.LENGTH_SHORT).show()
                                viewModel.fetchMyAlbums()
                            } else {
                                Toast.makeText(requireContext(), "Failed to create album", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun doUpload() {
        val title = binding.titleEditText.text?.toString()?.trim().orEmpty()
        val audioUri = selectedAudio
        val selectedAlbum = binding.albumSpinner.selectedItem as? Album

        if (title.isEmpty() || audioUri == null) {
            Toast.makeText(requireContext(), "Title and audio are required.", Toast.LENGTH_SHORT).show()
            return
        }

        setUploading(true)

        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val audioFile = FileUtils.copyUriToTempFile(requireContext(), audioUri, ".mp3")
                    val audioMime = FileUtils.getMime(requireContext(), audioUri) ?: "audio/mpeg"
                    val audioPart = MultipartBody.Part.createFormData("audio", audioFile.name, audioFile.asRequestBody(audioMime.toMediaTypeOrNull()))

                    val imagePart: MultipartBody.Part? = selectedImage?.let {
                        val imageFile = FileUtils.copyUriToTempFile(requireContext(), it, ".png")
                        val imageMime = FileUtils.getMime(requireContext(), it) ?: "image/png"
                        MultipartBody.Part.createFormData("image", imageFile.name, imageFile.asRequestBody(imageMime.toMediaTypeOrNull()))
                    }

                    val titleRB = title.toRequestBody("text/plain".toMediaTypeOrNull())
                    val albumIdRB = if (selectedAlbum != null && selectedAlbum.id != -1) {
                        selectedAlbum.id.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    } else null

                    val response = RetrofitInstance.api.uploadSong(titleRB, albumIdRB, audioPart, imagePart)
                    response.isSuccessful
                } catch (e: Exception) {
                    Log.e("UploadFragment", "Upload failed", e)
                    false
                }
            }

            setUploading(false)
            if (success) {
                Toast.makeText(requireContext(), "Uploaded!", Toast.LENGTH_SHORT).show()
                binding.titleEditText.text.clear()
                selectedAudio = null
                selectedImage = null
                binding.audioNameText.text = "Audio: none"
                binding.imageNameText.text = "Image: none (uses album art if available)"
                binding.albumSpinner.setSelection(0)
            } else {
                Toast.makeText(requireContext(), "Upload failed. Check logs for details.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setUploading(isUploading: Boolean) {
        binding.progressBar.isVisible = isUploading
        binding.uploadButton.isEnabled = !isUploading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}