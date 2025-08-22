package com.puggables.musically.ui.player

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.puggables.musically.MainViewModel
import com.puggables.musically.R
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.databinding.FragmentPlayerBinding

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlayerBinding.bind(view)

        binding.playerView.player = mainViewModel.mediaController

        mainViewModel.currentPlayingSong.observe(viewLifecycleOwner) { song ->
            if (song != null) {
                binding.songTitleTextView.text = song.title
                binding.artistNameTextView.text = song.artist
                // Use the dynamic base URL here too
                val coverUrl = song.imageUrl ?: "${RetrofitInstance.currentBaseUrl}static/images/${song.image}"
                binding.albumArtImageView.load(coverUrl)

                // tap to profile
                binding.artistNameTextView.setOnClickListener {
                    val artistId = song.artistId ?: return@setOnClickListener
                    // This will now resolve correctly because the nav graph is fixed
                    val action = PlayerFragmentDirections.actionPlayerFragmentToArtistProfileFragment(artistId)
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playerView.player = null
        _binding = null
    }
}
