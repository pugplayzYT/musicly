package com.puggables.musically.ui.player

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.puggables.musically.MainViewModel
import com.puggables.musically.NavGraphDirections
import com.puggables.musically.R
import com.puggables.musically.databinding.BottomsheetExpandedPlayerBinding
import androidx.media3.common.C
import coil.load
import kotlin.math.max

class ExpandedPlayerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetExpandedPlayerBinding? = null
    private val b get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()

    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false // Flag to check if user is dragging the seekbar

    private val progressRunnable = object : Runnable {
        override fun run() {
            val binding = _binding ?: return
            val player = mainViewModel.mediaController ?: return

            val dur = if (player.duration == C.TIME_UNSET) 0L else max(0L, player.duration)
            val pos = max(0L, player.currentPosition)

            if (dur > 0) {
                binding.positionSeekBar.max = dur.toInt()
                if (!isUserSeeking) {
                    binding.positionSeekBar.progress = pos.toInt()
                }
                binding.currentTimeText.text = format(pos)
                binding.timeLeftText.text = "-${format(max(0L, dur - pos))}"
            } else {
                binding.positionSeekBar.max = 1
                binding.positionSeekBar.progress = 0
                binding.currentTimeText.text = "0:00"
                binding.timeLeftText.text = "-0:00"
            }

            if (_binding != null && isAdded && dialog?.isShowing == true) {
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetExpandedPlayerBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // This part still needs the Material library because BottomSheetDialogFragment itself is a Material component.
        // But we have removed the Slider.
        dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            // ... (rest of the behavior setup is the same)
        }

        mainViewModel.currentPlayingSong.observe(viewLifecycleOwner) { song ->
            if (song != null) {
                b.titleText.text = song.title
                b.artistText.text = song.artist

                b.artistText.setOnClickListener {
                    song.artistId?.let { artistId ->
                        (activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)?.navController?.let { navController ->
                            val action = NavGraphDirections.actionGlobalArtistProfileFragment(artistId)
                            navController.navigate(action)
                            dismiss()
                        }
                    }
                }

                val coverUrl = song.imageUrl ?: "https://cents-mongolia-difficulties-mortgage.trycloudflare.com/static/images/${song.image}"
                b.coverImage.load(coverUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_music_note)
                    error(R.drawable.ic_music_note)
                }
            }
        }

        mainViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            b.playPauseButton.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        }

        b.playPauseButton.setOnClickListener {
            mainViewModel.currentPlayingSong.value?.let { song -> mainViewModel.playOrToggleSong(song) }
        }

        // Setup listener for the new SeekBar
        b.positionSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mainViewModel.mediaController?.seekTo(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
            }
        })

        handler.post(progressRunnable)
    }

    // ... (rest of the file: onStart, onDismiss, onDestroyView, cleanup, format) is the same ...
    override fun onStart() {
        super.onStart()
        if (_binding != null) handler.post(progressRunnable)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        cleanup()
    }

    override fun onDestroyView() {
        cleanup()
        super.onDestroyView()
    }

    private fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }

    private fun format(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val m = totalSec / 60
        val s = totalSec % 60
        return "$m:${s.toString().padStart(2, '0')}"
    }
}