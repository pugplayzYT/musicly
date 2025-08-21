package com.puggables.musically.ui.artist

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.puggables.musically.MainViewModel
import com.puggables.musically.R
import com.puggables.musically.data.models.Song
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.databinding.FragmentArtistProfileBinding
import com.puggables.musically.ui.home.SongAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ArtistProfileFragment : Fragment(R.layout.fragment_artist_profile) {

    private var _binding: FragmentArtistProfileBinding? = null
    private val binding get() = _binding!!
    private val args: ArtistProfileFragmentArgs by navArgs()
    private val vm: ArtistProfileVM by viewModels()
    private val mainVM: MainViewModel by activityViewModels()
    private val BASE = "https://cents-mongolia-difficulties-mortgage.trycloudflare.com"

    private lateinit var adapter: SongAdapter

    private var pickedImage: Uri? = null
    private var pickedAudio: Uri? = null

    private val pickNewImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedImage = uri
        Toast.makeText(requireContext(), if (uri!=null) "Image selected" else "No image", Toast.LENGTH_SHORT).show()
    }
    private val pickNewAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedAudio = uri
        Toast.makeText(requireContext(), if (uri!=null) "Audio selected" else "No audio", Toast.LENGTH_SHORT).show()
    }

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val f = copyToCache(uri, "avatar")
                val part = MultipartBody.Part.createFormData(
                    "avatar", f.name, f.asRequestBody("image/*".toMediaTypeOrNull())
                )
                val resp = RetrofitInstance.api.updateMyAvatar(part)
                CoroutineScope(Dispatchers.Main).launch {
                    if (resp.isSuccessful) {
                        loadArtist()
                        Toast.makeText(requireContext(), "Avatar updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentArtistProfileBinding.bind(view)

        adapter = SongAdapter(
            onSongClicked = { song ->
                mainVM.playOrToggleSong(song, isNewSong = true)
            },
            onArtistClicked = {
                // We are already on the artist's profile, so we don't need to do anything here.
                // Tapping the artist name again does nothing.
            }
        ).apply {
            setOnItemLongClickListener { song ->
                showOwnerMenu(song)
            }
        }

        binding.songsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.songsRecyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadArtist() }

        binding.changeAvatarButton.setOnClickListener {
            pickAvatar.launch("image/*")
        }

        loadArtist()
    }

    private fun loadArtist() {
        binding.swipeRefresh.isRefreshing = true
        vm.load(args.artistId) { artist, error ->
            binding.swipeRefresh.isRefreshing = false
            if (artist != null) {
                binding.artistName.text = artist.username
                binding.artistAvatar.load(artist.avatar_url ?: R.drawable.ic_person)
                adapter.songs = artist.songs

                val mine = artist.id == com.puggables.musically.MusicallyApplication.sessionManager.getUserId()
                binding.changeAvatarButton.isVisible = mine
                binding.ownerHint.isVisible = mine
            } else {
                Toast.makeText(requireContext(), error ?: "Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showOwnerMenu(song: Song) {
        val isOwner = song.artistId == com.puggables.musically.MusicallyApplication.sessionManager.getUserId()
        if (!isOwner) return
        EditSongBottomSheet(
            titleDefault = song.title,
            albumDefault = song.album,
            onPickImage = { pickNewImage.launch("image/*") },
            onPickAudio = { pickNewAudio.launch("audio/*") },
            onSave = { newTitle, newAlbum -> doEditSong(song.id, newTitle, newAlbum) },
            onDelete = { doDeleteSong(song.id) }
        ).show(parentFragmentManager, "edit_song")
    }

    private fun doEditSong(songId: Int, newTitle: String?, newAlbum: String?) {
        val titleRB = newTitle?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
        val albumRB = newAlbum?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imagePart = pickedImage?.let { uri ->
                    val f = copyToCache(uri, "cover")
                    MultipartBody.Part.createFormData("image", f.name, f.asRequestBody("image/*".toMediaTypeOrNull()))
                }
                val audioPart = pickedAudio?.let { uri ->
                    val f = copyToCache(uri, "song")
                    MultipartBody.Part.createFormData("audio", f.name, f.asRequestBody("audio/*".toMediaTypeOrNull()))
                }

                val resp = RetrofitInstance.api.editSong(songId, titleRB, albumRB, imagePart, audioPart)
                CoroutineScope(Dispatchers.Main).launch {
                    pickedImage = null; pickedAudio = null
                    if (resp.isSuccessful) {
                        Toast.makeText(requireContext(), "Updated", Toast.LENGTH_SHORT).show()
                        loadArtist()
                    } else Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun doDeleteSong(songId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val resp = RetrofitInstance.api.deleteSong(songId)
            CoroutineScope(Dispatchers.Main).launch {
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    loadArtist()
                } else Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyToCache(uri: Uri, prefix: String): File {
        val name = "${prefix}_${System.currentTimeMillis()}" // <-- fixed string template
        val out = File(requireContext().cacheDir, name)
        requireContext().contentResolver.openInputStream(uri).use { input ->
            out.outputStream().use { output -> input?.copyTo(output) }
        }
        return out
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
