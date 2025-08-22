package com.puggables.musically.ui.home

import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.puggables.musically.MainViewModel
import com.puggables.musically.R
import com.puggables.musically.databinding.FragmentHomeBinding
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
            onSongClicked = { song ->
                mainViewModel.playOrToggleSong(song, isNewSong = true)
            },
            onArtistClicked = { song ->
                song.artistId?.let { artistId ->
                    val action = HomeFragmentDirections.actionHomeFragmentToArtistProfileFragment(artistId)
                    findNavController().navigate(action)
                }
            }
        )
        adapter = songAdapter
        layoutManager = LinearLayoutManager(requireContext())
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