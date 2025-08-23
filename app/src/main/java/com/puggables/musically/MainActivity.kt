package com.puggables.musically

import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import coil.load
import com.google.common.util.concurrent.MoreExecutors
import com.puggables.musically.data.remote.RetrofitInstance
import com.puggables.musically.databinding.ActivityMainBinding
import com.puggables.musically.player.MusicService
import com.puggables.musically.ui.player.ExpandedPlayerBottomSheet

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController
    private var isOffline = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == navController.currentDestination?.id) {
                return@setOnItemSelectedListener false
            }

            when (item.itemId) {
                R.id.homeFragment, R.id.uploadFragment, R.id.settingsFragment, R.id.downloadsFragment -> {
                    navController.navigate(item.itemId)
                    true
                }
                R.id.profileMenuItem -> {
                    val userId = MusicallyApplication.sessionManager.getUserId()
                    if (userId != -1) {
                        val action = NavGraphDirections.actionGlobalArtistProfileFragment(userId)
                        navController.navigate(action)
                    }
                    true
                }
                else -> false
            }
        }

        // Simple toggle for offline mode simulation. Long-press the bottom nav bar.
        binding.bottomNavigation.setOnLongClickListener {
            isOffline = !isOffline
            binding.bottomNavigation.menu.findItem(R.id.downloadsFragment).isVisible = isOffline
            Toast.makeText(this, if (isOffline) "Offline Mode On" else "Online Mode On", Toast.LENGTH_SHORT).show()
            true
        }


        navController.addOnDestinationChangedListener { _, dest, _ ->
            val authScreens = setOf(R.id.splashFragment, R.id.loginFragment, R.id.registerFragment)
            val isAuth = dest.id in authScreens
            binding.bottomNavigation.isVisible = !isAuth
            if (isAuth) binding.miniPlayer.isVisible = false

            val menuItem = binding.bottomNavigation.menu.findItem(dest.id)
            if (menuItem != null) {
                menuItem.isChecked = true
            } else if (dest.id == R.id.artistProfileFragment) {
                binding.bottomNavigation.menu.findItem(R.id.profileMenuItem).isChecked = true
            }
        }

        binding.miniPlayer.setOnClickListener {
            ExpandedPlayerBottomSheet().show(supportFragmentManager, "expanded_player")
        }

        binding.miniPlayerPlayPause.setOnClickListener {
            mainViewModel.currentPlayingSong.value?.let { song ->
                mainViewModel.playOrToggleSong(song)
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        mainViewModel.currentPlayingSong.observe(this) { song ->
            binding.miniPlayer.isVisible = song != null
            song ?: return@observe

            binding.miniPlayerSongTitle.text = song.title
            binding.miniPlayerSongArtist.text = song.artist

            val coverUrl = song.imageUrl ?: "${RetrofitInstance.currentBaseUrl}static/images/${song.image}"
            binding.miniPlayerCover.load(coverUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_music_note)
                error(R.drawable.ic_music_note)
            }
        }

        mainViewModel.isPlaying.observe(this) { isPlaying ->
            binding.miniPlayerPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            mainViewModel.mediaController = future.get()
        }, MoreExecutors.directExecutor())
    }
}