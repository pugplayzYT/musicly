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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.puggables.musically.MainViewModel
import com.puggables.musically.R
import com.puggables.musically.data.models.Song
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.databinding.FragmentArtistProfileBinding
import com.puggables.musically.ui.home.SongAdapter
import com.puggables.musically.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private lateinit var albumAdapter: AlbumAdapter
    private lateinit var singlesAdapter: SongAdapter

    private var pickedImage: Uri? = null
    private var pickedAudio: Uri? = null

    private val pickNewImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedImage = uri
        Toast.makeText(requireContext(), if (uri != null) "Image selected" else "No image", Toast.LENGTH_SHORT).show()
    }
    private val pickNewAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedAudio = uri
        Toast.makeText(requireContext(), if (uri != null) "Audio selected" else "No audio", Toast.LENGTH_SHORT).show()
    }

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val f = copyToCache(uri, "avatar")
                val mimeType = FileUtils.getMime(requireContext(), uri) ?: "image/jpeg"
                val requestBody = f.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("avatar", f.name, requestBody)

                val resp = RetrofitInstance.api.updateMyAvatar(part)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful) {
                        loadArtist()
                        Toast.makeText(requireContext(), "Avatar updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Update failed: ${resp.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentArtistProfileBinding.bind(view)

        setupRecyclerViews()

        binding.swipeRefresh.setOnRefreshListener { loadArtist() }
        binding.changeAvatarButton.setOnClickListener { pickAvatar.launch("image/*") }

        loadArtist()
    }

    private fun setupRecyclerViews() {
        singlesAdapter = SongAdapter(
            onSongClicked = { song -> mainVM.playOrToggleSong(song, isNewSong = true) },
            onArtistClicked = { /* Already on profile, do nothing */ }
        ).apply {
            setOnItemLongClickListener { song -> showOwnerMenu(song) }
        }
        binding.singlesRecyclerView.apply {
            adapter = singlesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadArtist() {
        binding.swipeRefresh.isRefreshing = true
        vm.load(args.artistId) { artist, error ->
            binding.swipeRefresh.isRefreshing = false
            if (artist != null) {
                binding.artistName.text = artist.username
                binding.artistAvatar.load(artist.avatar_url ?: R.drawable.ic_person)

                val mine = artist.id == com.puggables.musically.MusicallyApplication.sessionManager.getUserId()
                binding.changeAvatarButton.isVisible = mine
                binding.ownerHint.isVisible = mine

                binding.albumsHeader.isVisible = artist.albums.isNotEmpty()
                binding.albumsRecyclerView.isVisible = artist.albums.isNotEmpty()
                albumAdapter = AlbumAdapter(
                    albums = artist.albums,
                    onSongClicked = { song -> mainVM.playOrToggleSong(song, isNewSong = true) },
                    onArtistClicked = {},
                    onSongLongClicked = { song -> showOwnerMenu(song) }
                )
                binding.albumsRecyclerView.apply {
                    adapter = albumAdapter
                    layoutManager = LinearLayoutManager(requireContext())
                }

                binding.singlesHeader.isVisible = artist.singles.isNotEmpty()
                binding.singlesRecyclerView.isVisible = artist.singles.isNotEmpty()
                singlesAdapter.songs = artist.singles

            } else {
                Toast.makeText(requireContext(), error ?: "Failed to load artist", Toast.LENGTH_SHORT).show()
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
        // This logic would need to be updated to handle changing albums, but for now it's ok
    }

    private fun doDeleteSong(songId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val resp = RetrofitInstance.api.deleteSong(songId)
            withContext(Dispatchers.Main) {
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                    loadArtist()
                } else {
                    Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun copyToCache(uri: Uri, prefix: String): File {
        val name = "${prefix}_${System.currentTimeMillis()}"
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