package com.puggables.musically.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.puggables.musically.MainViewModel
import com.puggables.musically.MusicallyApplication
import com.puggables.musically.R
import com.puggables.musically.data.models.Song
import com.puggables.musically.databinding.FragmentHomeBinding
import com.puggables.musically.downloading.DownloadService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var songAdapter: SongAdapter
    private var searchJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        setupRecyclerView()
        observeViewModels()

        binding.searchEditText.addTextChangedListener { editable ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(500L) // Debounce search
                val query = editable.toString().trim()
                if (query.isNotBlank()) {
                    homeViewModel.searchSongs(query)
                } else {
                    homeViewModel.fetchSongs()
                }
            }
        }
    }

    private fun setupRecyclerView() = binding.songsRecyclerView.apply {
        songAdapter = SongAdapter(
            viewLifecycleOwner.lifecycleScope, // Add this
            onSongClicked = { song ->
                mainViewModel.playOrToggleSong(song, isNewSong = true)
            },
            onArtistClicked = { song ->
                song.artistId?.let { artistId ->
                    val action = HomeFragmentDirections.actionHomeFragmentToArtistProfileFragment(artistId)
                    findNavController().navigate(action)
                }
            },
            onDownloadClicked = { song ->
                handleDownloadClick(song)
            }
        )
        adapter = songAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    private fun handleDownloadClick(song: Song) {
        if (!MusicallyApplication.sessionManager.isPro()) {
            Toast.makeText(requireContext(), "Musically Pro is required to download songs.", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.settingsFragment)
            return
        }

        val intent = Intent(requireContext(), DownloadService::class.java).apply {
            putExtra("songId", song.id)
            putExtra("title", song.title)
            putExtra("artist", song.artist)
            putExtra("album", song.album)
            putExtra("duration", song.duration)
            putExtra("audioUrl", song.streamUrl)
            putExtra("imageUrl", song.imageUrl)
            // Note: A better way to get the switch state would be from a shared ViewModel or SessionManager
            // For now, we'll assume a default.
            val downloadImages = true // You can connect this to the switch in settings later
            putExtra("downloadImages", downloadImages)
        }
        requireContext().startService(intent)
        Toast.makeText(requireContext(), "Starting download for ${song.title}", Toast.LENGTH_SHORT).show()
    }

    private fun observeViewModels() {
        homeViewModel.songs.observe(viewLifecycleOwner) { songs ->
            songAdapter.songs = songs
        }
        homeViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.songsRecyclerView.adapter = null
        _binding = null
    }
}
