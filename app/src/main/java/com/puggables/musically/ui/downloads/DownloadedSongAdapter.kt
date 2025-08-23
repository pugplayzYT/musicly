package com.puggables.musically.ui.downloads

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.puggables.musically.R
import com.puggables.musically.data.local.db.DownloadedSong
import com.puggables.musically.databinding.ItemDownloadedSongBinding
import java.io.File

class DownloadedSongAdapter(private val onSongClicked: (DownloadedSong) -> Unit) :
    ListAdapter<DownloadedSong, DownloadedSongAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(private val binding: ItemDownloadedSongBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: DownloadedSong, onSongClicked: (DownloadedSong) -> Unit) {
            binding.songTitleTextView.text = song.title
            binding.artistNameTextView.text = song.artist
            if (song.localImagePath != null) {
                binding.songCoverImageView.load(File(song.localImagePath)) {
                    placeholder(R.drawable.ic_music_note)
                    error(R.drawable.ic_music_note)
                }
            } else {
                binding.songCoverImageView.load(R.drawable.ic_music_note)
            }
            binding.root.setOnClickListener { onSongClicked(song) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDownloadedSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onSongClicked)
    }

    class DiffCallback : DiffUtil.ItemCallback<DownloadedSong>() {
        override fun areItemsTheSame(oldItem: DownloadedSong, newItem: DownloadedSong) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DownloadedSong, newItem: DownloadedSong) = oldItem == newItem
    }
}