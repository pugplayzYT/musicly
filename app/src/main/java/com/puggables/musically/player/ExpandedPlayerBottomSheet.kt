// app/src/main/java/com/puggables/musically/ui/player/ExpandedPlayerBottomSheet.kt
package com.puggables.musically.ui.player

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.puggables.musically.MainViewModel
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
    private val progressRunnable = object : Runnable {
        override fun run() {
            val binding = _binding ?: return
            val player = mainViewModel.mediaController ?: return

            val dur = if (player.duration == C.TIME_UNSET) 0L else max(0L, player.duration)
            val pos = max(0L, player.currentPosition)

            if (dur > 0) {
                binding.positionSlider.valueFrom = 0f
                binding.positionSlider.valueTo = dur.toFloat()
                // Only set programmatically if not being dragged by user to avoid jumpiness
                if (!binding.positionSlider.isPressed) {
                    binding.positionSlider.value = pos.toFloat()
                }
                binding.currentTimeText.text = format(pos)
                binding.timeLeftText.text = "-${format(max(0L, dur - pos))}"
            } else {
                // Live streams / unknown duration
                binding.positionSlider.valueFrom = 0f
                binding.positionSlider.valueTo = 1f
                binding.positionSlider.value = 0f
                binding.currentTimeText.text = "0:00"
                binding.timeLeftText.text = "-0:00"
            }

            if (_binding != null && isAdded && dialog?.isShowing == true) {
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetExpandedPlayerBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Expand fully and enable drag-to-close (no arrow button)
        dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isHideable = true
            behavior.skipCollapsed = false
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dismissAllowingStateLoss()
                    }
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }

        // Observe current song for header UI
        mainViewModel.currentPlayingSong.observe(viewLifecycleOwner) { song ->
            if (song != null) {
                b.titleText.text = song.title
                b.artistText.text = song.artist

                val coverUrl = song.imageUrl
                    ?: "https://cents-mongolia-difficulties-mortgage.trycloudflare.com/static/images/${song.image}"

                b.coverImage.load(coverUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_music_note)
                    error(R.drawable.ic_music_note)
                }
            }
        }

        // Play/Pause icon reflect state
        mainViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            b.playPauseButton.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }

        // Toggle play/pause
        b.playPauseButton.setOnClickListener {
            mainViewModel.currentPlayingSong.value?.let { song ->
                mainViewModel.playOrToggleSong(song)
            }
        }

        // Format the slider’s floating label as time LEFT (-m:ss) instead of raw ms
        b.positionSlider.setLabelFormatter { value ->
            val player = mainViewModel.mediaController
            val dur = player?.duration?.takeIf { it != C.TIME_UNSET } ?: 0L
            if (dur <= 0L) {
                "-0:00"
            } else {
                val remaining = max(0L, dur - value.toLong())
                "-${format(remaining)}"
            }
        }

        // Seek when user drags
        b.positionSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                mainViewModel.mediaController?.seekTo(value.toLong())
            }
        }

        // Start progress updates
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
