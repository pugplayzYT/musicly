package com.puggables.musically.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.puggables.musically.R
import com.puggables.musically.data.models.Song
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.databinding.ItemSongBinding

class SongAdapter(
    private val onSongClicked: (Song) -> Unit,
    private val onArtistClicked: (Song) -> Unit,
    private val onDownloadClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var onItemLongClick: ((Song) -> Unit)? = null
    fun setOnItemLongClickListener(cb: (Song) -> Unit) { onItemLongClick = cb }

    inner class SongViewHolder(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root)

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
        val song = songs[position]
        holder.binding.apply {
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

            downloadButton.setOnClickListener { onDownloadClicked(song) }

            root.setOnClickListener { onSongClicked(song) }
            root.setOnLongClickListener {
                onItemLongClick?.invoke(song)
                onItemLongClick != null
            }
        }
    }

    override fun getItemCount() = songs.size
}