package com.lingualink.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingualink.BuildConfig
import com.lingualink.data.datastore.SettingsDataStore
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
    private val settingsDataStore: SettingsDataStore,
    val updateManager: UpdateManager
) : ViewModel() {
    var apiEndpoint by mutableStateOf("")
    var apiKey by mutableStateOf("")
    var selectedModel by mutableStateOf("deepseek-chat")
    var checkUpdates by mutableStateOf(true)
    var availableModels by mutableStateOf<List<String>>(emptyList())
    var isLoadingModels by mutableStateOf(false)
    var modelError by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            apiEndpoint = settingsDataStore.getApiEndpoint()
            apiKey = settingsDataStore.getApiKey()
            selectedModel = settingsDataStore.getSelectedModel()
            checkUpdates = settingsDataStore.getCheckUpdates()
            // Also sync to engine
            onlineEngine.apiEndpoint = apiEndpoint
            onlineEngine.apiKey = apiKey
            onlineEngine.selectedModel = selectedModel
        }
    }

    fun saveApiSettings() {
        onlineEngine.apiEndpoint = apiEndpoint
        onlineEngine.apiKey = apiKey
        onlineEngine.selectedModel = selectedModel
        viewModelScope.launch {
            onlineEngine.saveSettings()
        }
    }

    fun fetchModels() {
        saveApiSettings()
        isLoadingModels = true
        modelError = null
        viewModelScope.launch {
            try {
                availableModels = onlineEngine.fetchModels()
            } catch (e: Exception) {
                modelError = e.message
            } finally {
                isLoadingModels = false
            }
        }
    }

    fun checkForUpdate() {
        val repo = BuildConfig.GITHUB_REPO
        if (repo.isBlank() || repo == "OWNER/lingualink") return
        viewModelScope.launch {
            updateManager.checkForUpdate(repo)
        }
    }

    fun persistCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCheckUpdates(enabled)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val updateState by viewModel.updateManager.updateState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf<UpdateInfo?>(null) }
    var showApiKey by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.checkUpdates) viewModel.checkForUpdate()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium)

        // API Settings
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("翻译 API 配置", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = viewModel.apiEndpoint,
                    onValueChange = { viewModel.apiEndpoint = it },
                    label = { Text("API 端点") },
                    placeholder = { Text("https://api.deepseek.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Language, null) }
                )
                OutlinedTextField(
                    value = viewModel.apiKey,
                    onValueChange = { viewModel.apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = { Icon(Icons.Default.Key, null) },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "隐藏" else "显示"
                            )
                        }
                    }
                )

                // Model selector with fetch button
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = modelExpanded && viewModel.availableModels.isNotEmpty(),
                        onExpandedChange = {
                            if (viewModel.availableModels.isNotEmpty()) modelExpanded = !modelExpanded
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = viewModel.selectedModel,
                            onValueChange = { viewModel.selectedModel = it },
                            label = { Text("模型") },
                            placeholder = { Text("deepseek-chat") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.SmartToy, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            viewModel.availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model, fontSize = 14.sp) },
                                    onClick = {
                                        viewModel.selectedModel = model
                                        modelExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { viewModel.fetchModels() },
                        enabled = !viewModel.isLoadingModels,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        if (viewModel.isLoadingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("获取", fontSize = 13.sp)
                    }
                }
                viewModel.modelError?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.saveApiSettings()
                        Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("保存配置") }
            }
        }

        // Update
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("应用更新", style = MaterialTheme.typography.titleMedium)
                Text("当前版本: v${viewModel.updateManager.currentVersion}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("启动时检查更新", modifier = Modifier.weight(1f))
                    Switch(checked = viewModel.checkUpdates, onCheckedChange = {
                        viewModel.checkUpdates = it
                        viewModel.persistCheckUpdates(it)
                    })
                }
                OutlinedButton(onClick = { viewModel.checkForUpdate() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
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
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("关于", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("LinguaLink - 多设备联机翻译")
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "使用 Xiaomi MiMo V2.5 Pro 开发",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
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
