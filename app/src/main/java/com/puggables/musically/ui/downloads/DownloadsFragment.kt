package com.puggables.musically.ui.downloads

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.puggables.musically.MainViewModel
import com.puggables.musically.R
import com.puggables.musically.data.local.db.DownloadedSong
import com.puggables.musically.data.models.Song
import com.puggables.musically.databinding.FragmentDownloadsBinding

class DownloadsFragment : Fragment(R.layout.fragment_downloads) {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DownloadsViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels() // Get the shared MainViewModel
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
        downloadedAdapter = DownloadedSongAdapter { downloadedSong ->
            // When a downloaded song is clicked, convert it to a regular Song object
            // and tell the MainViewModel to play it.
            val songToPlay = downloadedSong.toSong()
            mainViewModel.playOrToggleSong(songToPlay, isNewSong = true)
        }
        binding.downloadsRecyclerView.apply {
            adapter = downloadedAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // Helper function to convert a DownloadedSong to a Song
    private fun DownloadedSong.toSong(): Song {
        return Song(
            id = this.id,
            title = this.title,
            artist = this.artist,
            album = this.album,
            image = this.localImagePath ?: "", // Use local image path
            filepath = this.localAudioPath, // Use local audio path
            duration = this.duration,
            artistId = null, // Not needed for playback
            imageUrl = this.localImagePath, // Pass local path for the player UI
            streamUrl = this.localAudioPath // Pass local path as the stream URL
        )
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
