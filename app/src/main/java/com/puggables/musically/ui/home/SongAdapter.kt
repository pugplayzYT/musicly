package com.puggables.musically.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.puggables.musically.R
import com.puggables.musically.data.models.Song
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.databinding.ItemSongBinding
import com.puggables.musically.downloading.DownloadStatus
import com.puggables.musically.downloading.DownloadTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SongAdapter(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onSongClicked: (Song) -> Unit,
    private val onArtistClicked: (Song) -> Unit,
    private val onDownloadClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var onItemLongClick: ((Song) -> Unit)? = null
    fun setOnItemLongClickListener(cb: (Song) -> Unit) { onItemLongClick = cb }

    inner class SongViewHolder(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root) {
        private var downloadStatusJob: Job? = null

        fun bind(song: Song) {
            binding.apply {
                songTitleTextView.text = song.title
                artistNameTextView.text = song.artist
                artistNameTextView.setOnClickListener { onArtistClicked(song) }

                val baseUrl = RetrofitInstance.currentBaseUrl
                val imageUrl = song.imageUrl ?: ("${baseUrl}static/images/" + song.image)
                songCoverImageView.load(imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_music_note)
                    error(R.drawable.ic_music_note)
                }

                root.setOnClickListener { onSongClicked(song) }
                root.setOnLongClickListener {
                    onItemLongClick?.invoke(song)
                    onItemLongClick != null
                }

                // Cancel any previous collector job before starting a new one
                downloadStatusJob?.cancel()
                // Start a new coroutine to collect download status updates for this specific song
                downloadStatusJob = lifecycleScope.launch {
                    DownloadTracker.getDownloadStatusFlow(song.id).collectLatest { status ->
                        updateDownloadUI(status, song)
                    }
                }
                // Tag the job to the view for recycling
                root.tag = downloadStatusJob
            }
        }

        private fun updateDownloadUI(status: DownloadStatus, song: Song) {
            binding.apply {
                when (status) {
                    is DownloadStatus.Idle -> {
                        downloadProgressBar.visibility = View.GONE
                        downloadButton.visibility = View.VISIBLE
                        downloadButton.setImageResource(R.drawable.ic_download)
                        downloadButton.setOnClickListener { onDownloadClicked(song) }
                    }
                    is DownloadStatus.Downloading -> {
                        downloadProgressBar.visibility = View.VISIBLE
                        downloadButton.visibility = View.VISIBLE // Keep button visible
                        downloadButton.setImageResource(android.R.color.transparent) // Hide icon
                        downloadProgressBar.progress = status.progress
                        downloadButton.setOnClickListener(null) // Disable clicks while downloading
                    }
                    is DownloadStatus.Downloaded -> {
                        downloadProgressBar.visibility = View.GONE
                        downloadButton.visibility = View.VISIBLE
                        downloadButton.setImageResource(R.drawable.ic_check) // A checkmark icon
                        downloadButton.setOnClickListener {
                            Toast.makeText(root.context, "Song already downloaded", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private val diffCallback = object : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Song, newItem: Song) = oldItem == newItem
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    var songs: List<Song>
        get() = differ.currentList
        set(value) { differ.submitList(value) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position])
    }

    override fun onViewRecycled(holder: SongViewHolder) {
        super.onViewRecycled(holder)
        // This is crucial to stop collecting when the view is no longer visible
        (holder.binding.root.tag as? Job)?.cancel()
    }


    override fun getItemCount() = songs.size
}
