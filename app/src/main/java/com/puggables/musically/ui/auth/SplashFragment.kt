package com.puggables.musically.ui.auth

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.puggables.musically.R
import com.puggables.musically.MusicallyApplication
import com.puggables.musically.data.remote.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var downloadId: Long = -1
    private lateinit var downloadManager: DownloadManager

    // UI Elements for progress
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var downloadProgressLayout: LinearLayout
    private lateinit var downloadProgressBar: ProgressBar
    private lateinit var downloadProgressText: TextView

    // --- NEW: A flag to prevent triggering the install twice ---
    @Volatile
    private var installTriggered = false

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId && isAdded) {
                // The broadcast is a backup, the main logic is now in the progress loop
                triggerApkInstall()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        downloadProgressLayout = view.findViewById(R.id.downloadProgressLayout)
        downloadProgressBar = view.findViewById(R.id.downloadProgressBar)
        downloadProgressText = view.findViewById(R.id.downloadProgressText)

        downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireActivity().registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        checkForUpdates()
    }

    private fun checkForUpdates() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitInstance.api.getVersionInfo()
                if (response.isSuccessful) {
                    val remoteConfig = response.body()
                    val latestVersionCode = (remoteConfig?.get("latest_version_code") as? Double)?.toInt() ?: 1
                    val forceUpdate = remoteConfig?.get("force_update") as? Boolean ?: false
                    val apkUrl = remoteConfig?.get("apk_url") as? String

                    val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                    val currentVersionCode = packageInfo.versionCode

                    if (forceUpdate && currentVersionCode < latestVersionCode && !apkUrl.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            showForceUpdateDialog(apkUrl)
                        }
                    } else {
                        proceedToApp()
                    }
                } else {
                    proceedToApp()
                }
            } catch (e: Exception) {
                proceedToApp()
            }
        }
    }

    private fun proceedToApp() {
        lifecycleScope.launch(Dispatchers.Main) {
            delay(500)
            if (!isAdded) return@launch
            if (MusicallyApplication.sessionManager.isLoggedIn()) {
                findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
            } else {
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            }
        }
    }

    private fun showForceUpdateDialog(apkUrl: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Update Required")
            .setMessage("A new version of the app is available. Please update to continue.")
            .setCancelable(false)
            .setPositiveButton("Update") { _, _ ->
                downloadAndInstallApk(apkUrl)
            }
            .show()
    }

    private fun downloadAndInstallApk(apkUrl: String) {
        val fileName = "musically-update.apk"
        val destination = requireContext().getExternalFilesDir(null)?.resolve(fileName)

        if (destination?.exists() == true) {
            destination.delete()
        }

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Musically Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType("application/vnd.android.package-archive")
            .setDestinationUri(Uri.fromFile(destination))

        downloadId = downloadManager.enqueue(request)

        loadingSpinner.isVisible = false
        downloadProgressLayout.isVisible = true

        lifecycleScope.launch {
            updateDownloadProgress()
        }
    }

    // --- NEW: Centralized function to trigger the install ---
    private fun triggerApkInstall() {
        if (installTriggered || !isAdded) return
        synchronized(this) {
            if (installTriggered) return
            installTriggered = true
        }

        val apkFile = requireContext().getExternalFilesDir(null)?.resolve("musically-update.apk")

        if (apkFile != null && apkFile.exists()) {
            val apkUri: Uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(installIntent)
        } else {
            Toast.makeText(context, "Update file not found. Download failed.", Toast.LENGTH_SHORT).show()
            proceedToApp()
        }
    }

    private suspend fun updateDownloadProgress() {
        var isDownloading = true
        while (isDownloading && lifecycleScope.isActive) {
            downloadManager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val downloadedBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = cursor.getInt(statusIndex)
                    val downloaded = cursor.getLong(downloadedBytesIndex)
                    val total = cursor.getLong(totalBytesIndex)

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        isDownloading = false
                        // --- FIX: Trigger install directly from the loop ---
                        withContext(Dispatchers.Main) {
                            downloadProgressBar.progress = 100
                            downloadProgressText.text = "Download complete. Starting installer..."
                            triggerApkInstall()
                        }
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        isDownloading = false
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Update download failed.", Toast.LENGTH_SHORT).show()
                            proceedToApp()
                        }
                    }

                    val progress = if (total > 0) ((downloaded * 100L) / total).toInt() else -1

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        if (progress >= 0) {
                            downloadProgressBar.isIndeterminate = false
                            downloadProgressBar.progress = progress
                        } else {
                            downloadProgressBar.isIndeterminate = true
                        }
                        val downloadedMb = String.format("%.1f", downloaded / 1024.0 / 1024.0)
                        val totalMb = String.format("%.1f", total / 1024.0 / 1024.0)
                        downloadProgressText.text = if (total > 0) "$downloadedMb MB / $totalMb MB ($progress%)" else "Downloading..."
                    }
                } else {
                    isDownloading = false
                }
            }
            delay(500)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().unregisterReceiver(onDownloadComplete)
    }
}