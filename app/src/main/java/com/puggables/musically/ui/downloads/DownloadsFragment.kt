package com.puggables.musically.ui.downloads

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.puggables.musically.R
import com.puggables.musically.databinding.FragmentDownloadsBinding

class DownloadsFragment : Fragment(R.layout.fragment_downloads) {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DownloadsViewModel by viewModels()
    private lateinit var downloadedAdapter: DownloadedSongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDownloadsBinding.bind(view)

        setupRecyclerView()

        viewModel.downloadedSongs.observe(viewLifecycleOwner) { songs ->
            downloadedAdapter.submitList(songs)
        }
    }

    private fun setupRecyclerView() {
        downloadedAdapter = DownloadedSongAdapter { song ->
            // TODO: Implement offline playback
        }
        binding.downloadsRecyclerView.apply {
            adapter = downloadedAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}