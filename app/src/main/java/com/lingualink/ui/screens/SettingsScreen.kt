package com.lingualink.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingualink.BuildConfig
import com.lingualink.translation.OnlineTranslationEngine
import com.lingualink.update.UpdateInfo
import com.lingualink.update.UpdateManager
import com.lingualink.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val onlineEngine: OnlineTranslationEngine,
    val updateManager: UpdateManager
) : ViewModel() {
    var apiEndpoint by mutableStateOf(onlineEngine.apiEndpoint)
    var apiKey by mutableStateOf(onlineEngine.apiKey)
    var selectedModel by mutableStateOf(onlineEngine.selectedModel)
    var checkUpdates by mutableStateOf(true)

    fun saveApiSettings() {
        onlineEngine.apiEndpoint = apiEndpoint
        onlineEngine.apiKey = apiKey
        onlineEngine.selectedModel = selectedModel
    }

    fun checkForUpdate() {
        val repo = BuildConfig.GITHUB_REPO
        if (repo.isBlank() || repo == "OWNER/lingualink") return
        viewModelScope.launch {
            updateManager.checkForUpdate(repo)
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val updateState by viewModel.updateManager.updateState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf<UpdateInfo?>(null) }

    // Auto-check on first composition
    LaunchedEffect(Unit) {
        if (viewModel.checkUpdates) viewModel.checkForUpdate()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)

        // API Settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("翻译 API 配置", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = viewModel.apiEndpoint,
                    onValueChange = { viewModel.apiEndpoint = it },
                    label = { Text("API 端点") },
                    placeholder = { Text("https://api.deepseek.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.apiKey,
                    onValueChange = { viewModel.apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = viewModel.selectedModel,
                    onValueChange = { viewModel.selectedModel = it },
                    label = { Text("模型") },
                    placeholder = { Text("deepseek-chat") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        viewModel.saveApiSettings()
                        Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存") }
            }
        }

        // Update
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("应用更新", style = MaterialTheme.typography.titleMedium)
                Text("当前版本: v${viewModel.updateManager.currentVersion}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("启动时检查更新", modifier = Modifier.weight(1f))
                    Switch(checked = viewModel.checkUpdates, onCheckedChange = { viewModel.checkUpdates = it })
                }
                Button(onClick = { viewModel.checkForUpdate() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.SystemUpdate, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("检查更新")
                }
                when (val state = updateState) {
                    is UpdateState.Available -> { showUpdateDialog = state.info }
                    is UpdateState.Downloading -> {
                        LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("下载中... ${state.progress}%", fontSize = 12.sp)
                    }
                    is UpdateState.Error -> { Text(state.message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    else -> {}
                }
            }
        }

        // About
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("LinguaLink - 多设备联机翻译")
                Text("v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    showUpdateDialog?.let { info ->
        AlertDialog(
            onDismissRequest = { showUpdateDialog = null },
            title = { Text("发现新版本 v${info.version}") },
            text = { Column { Text("更新内容:"); Spacer(Modifier.height(8.dp)); Text(info.releaseNotes, fontSize = 13.sp) } },
            confirmButton = { TextButton(onClick = { viewModel.updateManager.downloadAndInstall(info); showUpdateDialog = null }) { Text("下载更新") } },
            dismissButton = { TextButton(onClick = { showUpdateDialog = null }) { Text("稍后") } }
        )
    }
}
