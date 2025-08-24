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
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.puggables.musically.R
import com.puggables.musically.MusicallyApplication
import com.puggables.musically.data.remote.RetrofitInstance
import kotlinx.coroutines.launch
import java.io.File

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var downloadId: Long = -1

    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val uri = downloadManager.getUriForDownloadedFile(id)
                if (uri != null) {
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(installIntent)
                } else {
                    Toast.makeText(context, "Download failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // THIS IS THE FIX
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireActivity().registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }


        lifecycleScope.launch {
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
                        showForceUpdateDialog(apkUrl)
                    } else {
                        proceedToApp()
                    }
                } else {
                    proceedToApp()
                }
            } catch (e: Exception) {
                // If there's an error fetching, let the user in.
                proceedToApp()
            }
        }
    }

    private fun proceedToApp() {
        if (MusicallyApplication.sessionManager.isLoggedIn()) {
            findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
        } else {
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
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
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Musically Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "musically.apk")

        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        Toast.makeText(requireContext(), "Downloading update...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(onDownloadComplete)
    }
}