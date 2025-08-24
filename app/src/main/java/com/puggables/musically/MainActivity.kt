package com.puggables.musically

import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
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
    private lateinit var connectivityManager: ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            runOnUiThread {
                updateNavMenuForConnectivity(true)
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            runOnUiThread {
                updateNavMenuForConnectivity(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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

        navController.addOnDestinationChangedListener { _, dest, _ ->
            val authScreens = setOf(R.id.splashFragment, R.id.loginFragment, R.id.registerFragment)
            val isLoggedIn = MusicallyApplication.sessionManager.isLoggedIn()

            binding.bottomNavigation.isVisible = isLoggedIn && dest.id !in authScreens

            if (!isLoggedIn || dest.id in authScreens) {
                binding.miniPlayer.isVisible = false
            }

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

    private fun updateNavMenuForConnectivity(isConnected: Boolean) {
        binding.bottomNavigation.menu.findItem(R.id.downloadsFragment).isVisible = !isConnected
        binding.bottomNavigation.menu.findItem(R.id.uploadFragment).isVisible = isConnected

        // If offline, and the current screen is the upload screen, navigate home
        if (!isConnected && navController.currentDestination?.id == R.id.uploadFragment) {
            navController.navigate(R.id.homeFragment)
        }
    }

    // Helper function to check the current network state
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun observeViewModel() {
        mainViewModel.currentPlayingSong.observe(this) { song ->
            binding.miniPlayer.isVisible = song != null && MusicallyApplication.sessionManager.isLoggedIn()
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
        // Register the callback to listen for future changes
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        // FIX: Check the initial network state when the activity starts
        val isConnected = isNetworkAvailable()
        updateNavMenuForConnectivity(isConnected)

        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            mainViewModel.mediaController = future.get()
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        super.onStop()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
