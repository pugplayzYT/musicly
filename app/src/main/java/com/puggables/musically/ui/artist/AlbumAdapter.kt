package com.puggables.musically.ui.artist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.puggables.musically.data.models.Album
import com.puggables.musically.data.models.Song
import com.puggables.musically.databinding.ItemAlbumWithSongsBinding
import com.puggables.musically.ui.home.SongAdapter

class AlbumAdapter(
    private var albums: List<Album>,
    private val onSongClicked: (Song) -> Unit,
    private val onArtistClicked: (Song) -> Unit,
    private val onSongLongClicked: (Song) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>() {

    inner class AlbumViewHolder(val binding: ItemAlbumWithSongsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val binding = ItemAlbumWithSongsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlbumViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val album = albums[position]
        holder.binding.albumTitleTextView.text = album.title

        // This correctly creates the SongAdapter without passing songs in the constructor
        val songAdapter = SongAdapter(onSongClicked, onArtistClicked).apply {
            setOnItemLongClickListener(onSongLongClicked)
        }

        holder.binding.songsInAlbumRecyclerView.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(holder.itemView.context)
        }
        // This sets the songs property after creation
        songAdapter.songs = album.songs ?: emptyList()
    }

    override fun getItemCount() = albums.size
}