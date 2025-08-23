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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.puggables.musically.MainViewModel
import com.puggables.musically.NavGraphDirections
import com.puggables.musically.R
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.databinding.BottomsheetExpandedPlayerBinding
import androidx.media3.common.C
import coil.load
import kotlin.math.max

class ExpandedPlayerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetExpandedPlayerBinding? = null
    private val b get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()

    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false

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
        dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
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
                val baseUrl = RetrofitInstance.currentBaseUrl
                val coverUrl = song.imageUrl ?: "${baseUrl}static/images/${song.image}"
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

        b.positionSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mainViewModel.mediaController?.seekTo(progress.toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) { isUserSeeking = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { isUserSeeking = false }
        })

        mainViewModel.playbackSpeed.observe(viewLifecycleOwner) { speed ->
            b.speedButton.text = "${speed}x"
        }

        b.speedButton.setOnClickListener {
            val speedOptions = arrayOf("1.0x", "1.25x", "1.5x", "2.0x", "3.5x")
            val speedValues = floatArrayOf(1.0f, 1.25f, 1.5f, 2.0f, 3.5f)
            val currentSpeed = mainViewModel.playbackSpeed.value ?: 1.0f
            val checkedItem = speedValues.indexOfFirst { it == currentSpeed }.coerceAtLeast(0)

            AlertDialog.Builder(requireContext())
                .setTitle("Select Playback Speed")
                .setSingleChoiceItems(speedOptions, checkedItem) { dialog, which ->
                    mainViewModel.setPlaybackSpeed(speedValues[which])
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        handler.post(progressRunnable)
    }

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