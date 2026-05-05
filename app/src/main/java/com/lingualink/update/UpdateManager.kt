package com.lingualink.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    private var downloadId: Long = -1
    private var downloadScope: CoroutineScope? = null

    val currentVersion: String
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: Exception) { "0.0.0" }

    /**
     * Check GitHub releases for updates.
     * Returns UpdateInfo if a newer version is available, null otherwise.
     */
    suspend fun checkForUpdate(repo: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$repo/releases/latest"
            val request = Request.Builder().url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val release = json.decodeFromString<GitHubRelease>(body)

            if (release.draft || release.prerelease) return@withContext null

            val latestVersion = release.tagName.removePrefix("v")
            if (!isNewerVersion(currentVersion, latestVersion)) return@withContext null

            // Find APK asset
            val apkAsset = release.assets.find {
                it.name.endsWith(".apk") && !it.name.contains("debug")
            } ?: release.assets.find { it.name.endsWith(".apk") }

            if (apkAsset == null) {
                _updateState.value = UpdateState.NoUpdate
                return@withContext null
            }

            val info = UpdateInfo(
                version = latestVersion,
                releaseNotes = release.body ?: "",
                downloadUrl = apkAsset.browserDownloadUrl,
                fileName = apkAsset.name,
                publishedAt = release.publishedAt
            )
            _updateState.value = UpdateState.Available(info)
            info
        } catch (e: Exception) {
            Log.w("UpdateManager", "Check failed: ${e.message}")
            null
        }
    }

    /**
     * Download the APK and trigger installation.
     */
    fun downloadAndInstall(updateInfo: UpdateInfo) {
        _updateState.value = UpdateState.Downloading(0)

        val file = File(context.getExternalFilesDir(null), "updates/${updateInfo.fileName}")
        file.parentFile?.mkdirs()

        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
            .setTitle("LinguaLink 更新")
            .setDescription("正在下载 v${updateInfo.version}")
            .setDestinationUri(Uri.fromFile(file))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)

        // Register receiver for download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    try { ctx.unregisterReceiver(this) } catch (_: Exception) {}
                    installApk(file)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        // Poll progress using coroutine
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        downloadScope = scope
        scope.launch {
            var downloading = true
            while (isActive && downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    if (total > 0) {
                        val progress = (downloaded * 100 / total).toInt()
                        _updateState.value = UpdateState.Downloading(progress)
                    }
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                        if (status == DownloadManager.STATUS_FAILED) {
                            _updateState.value = UpdateState.Error("下载失败")
                        }
                        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                    }
                }
                cursor?.close()
                delay(500)
            }
            downloadScope = null
        }
    }

    private fun installApk(file: File) {
        _updateState.value = UpdateState.Installing
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isNewerVersion(current: String, latest: String): Boolean =
        VersionUtils.isNewerVersion(current, latest)
}

data class UpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val fileName: String,
    val publishedAt: String
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data object NoUpdate : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data object Installing : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@Serializable
private data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val publishedAt: String = "",
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
private data class GitHubAsset(
    val name: String,
    val browserDownloadUrl: String,
    val size: Long = 0
)
