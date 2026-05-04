package com.lingualink.ui.viewmodel

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lingualink.data.db.dao.TranslationDao
import com.lingualink.data.db.entity.TranslationEntity
import com.lingualink.domain.model.DeviceInfo
import com.lingualink.domain.model.TranslationMode
import com.lingualink.domain.model.TranslationRequest
import com.lingualink.network.client.DeviceHttpClient
import com.lingualink.network.discovery.MulticastDiscovery
import com.lingualink.network.dto.MulticastDto
import com.lingualink.network.dto.TranslateRequestDto
import com.lingualink.network.dto.TranslateResultDto
import com.lingualink.network.server.FanyiHttpServer
import com.lingualink.translation.OfflineTranslationEngine
import com.lingualink.translation.OnlineTranslationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val translatedText: String? = null,
    val senderAlias: String,
    val senderId: String,
    val isLocal: Boolean,
    val sourceLang: String = "",
    val targetLang: String = "",
    val mode: TranslationMode? = null,
    val isTranslating: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class HomeUiState(
    val messages: List<ChatMessage> = emptyList(),
    val discoveredDevices: List<DeviceInfo> = emptyList(),
    val connectedDevices: List<DeviceInfo> = emptyList(),
    val isScanning: Boolean = false,
    val isServerRunning: Boolean = false,
    val sourceLang: String = "zh",
    val targetLang: String = "en",
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val deviceClient: DeviceHttpClient,
    private val onlineEngine: OnlineTranslationEngine,
    private val offlineEngine: OfflineTranslationEngine,
    private val translationDao: TranslationDao
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val fingerprint: String = try {
        Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
            ?: UUID.randomUUID().toString()
    } catch (e: Exception) {
        UUID.randomUUID().toString()
    }

    private val deviceAlias: String = android.os.Build.MODEL ?: "Android"

    private var httpServer: FanyiHttpServer? = null
    private var multicastDiscovery: MulticastDiscovery? = null

    init {
        try {
            startServer()
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to start server", e)
            _uiState.value = _uiState.value.copy(errorMessage = "服务启动失败: ${e.message}")
        }
    }

    private fun startServer() {
        val server = FanyiHttpServer(scope = viewModelScope)
        server.getDeviceInfo = {
            com.lingualink.network.dto.DeviceInfoDto(
                alias = deviceAlias,
                deviceModel = android.os.Build.MODEL ?: "Unknown",
                fingerprint = fingerprint,
                port = DeviceInfo.DEFAULT_PORT
            )
        }
        server.onTranslateRequest = { request -> handleIncomingRequest(request) }
        server.onTranslateResult = { result -> handleIncomingResult(result) }
        server.start()
        httpServer = server
        _uiState.value = _uiState.value.copy(isServerRunning = true)
    }

    fun startScan() {
        if (_uiState.value.isScanning) return
        _uiState.value = _uiState.value.copy(isScanning = true, discoveredDevices = emptyList())
        val discovery = MulticastDiscovery(
            deviceClient = deviceClient,
            scope = viewModelScope
        )
        multicastDiscovery = discovery

        viewModelScope.launch {
            discovery.discovered.collect { device ->
                val current = _uiState.value.discoveredDevices.toMutableList()
                if (current.none { it.fingerprint == device.fingerprint }
                    && device.fingerprint != fingerprint) {
                    current.add(device)
                    _uiState.value = _uiState.value.copy(discoveredDevices = current)
                }
            }
        }

        discovery.startListener(fingerprint) { buildMulticastDto() }
        discovery.sendAnnouncement(buildMulticastDto())

        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            _uiState.value = _uiState.value.copy(isScanning = false)
        }
    }

    fun connectDevice(device: DeviceInfo) {
        val state = _uiState.value
        if (state.connectedDevices.any { it.fingerprint == device.fingerprint }) return
        val updatedConnected = state.connectedDevices + device
        val updatedDiscovered = state.discoveredDevices.filter { it.fingerprint != device.fingerprint }
        _uiState.value = state.copy(
            connectedDevices = updatedConnected,
            discoveredDevices = updatedDiscovered
        )
    }

    fun disconnectDevice(device: DeviceInfo) {
        val state = _uiState.value
        val updatedConnected = state.connectedDevices.filter { it.fingerprint != device.fingerprint }
        val updatedDiscovered = state.discoveredDevices + device
        _uiState.value = state.copy(
            connectedDevices = updatedConnected,
            discoveredDevices = updatedDiscovered
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun setSourceLang(lang: String) {
        _uiState.value = _uiState.value.copy(sourceLang = lang)
    }

    fun setTargetLang(lang: String) {
        _uiState.value = _uiState.value.copy(targetLang = lang)
    }

    fun swapLanguages() {
        val state = _uiState.value
        _uiState.value = state.copy(sourceLang = state.targetLang, targetLang = state.sourceLang)
    }

    fun sendTranslation(text: String) {
        if (text.isBlank()) return
        val state = _uiState.value

        val localMsg = ChatMessage(
            text = text,
            senderAlias = deviceAlias,
            senderId = fingerprint,
            isLocal = true,
            sourceLang = state.sourceLang,
            targetLang = state.targetLang,
            isTranslating = true
        )
        _uiState.value = state.copy(messages = state.messages + localMsg)

        viewModelScope.launch {
            try {
                val engine = selectEngine()
                val result = engine.translate(
                    TranslationRequest(text, state.sourceLang, state.targetLang)
                )
                val updatedMsg = localMsg.copy(
                    translatedText = result.translatedText,
                    mode = result.mode,
                    isTranslating = false
                )
                updateMessage(updatedMsg)
                saveTranslation(updatedMsg, result.latencyMs)

                val resultDto = TranslateResultDto(
                    requestId = localMsg.id,
                    senderId = fingerprint,
                    originalText = text,
                    translatedText = result.translatedText,
                    sourceLang = state.sourceLang,
                    targetLang = state.targetLang,
                    latencyMs = result.latencyMs,
                    translationMode = result.mode.name.lowercase()
                )
                _uiState.value.connectedDevices.forEach { device ->
                    deviceClient.sendTranslationResult(device, resultDto)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Translation failed", e)
                updateMessage(localMsg.copy(isTranslating = false))
                _uiState.value = _uiState.value.copy(
                    errorMessage = "翻译失败: ${e.message}"
                )
            }
        }
    }

    private fun selectEngine() = when {
        onlineEngine.isAvailable() -> onlineEngine
        offlineEngine.isAvailable() -> offlineEngine
        else -> throw UnsupportedOperationException("请在设置中配置 API，或使用支持离线的语言对（中↔英、英↔日）")
    }

    private suspend fun saveTranslation(msg: ChatMessage, latencyMs: Long) {
        try {
            translationDao.insert(
                TranslationEntity(
                    requestId = msg.id,
                    sessionId = "",
                    senderDeviceId = msg.senderId,
                    senderAlias = msg.senderAlias,
                    originalText = msg.text,
                    translatedText = msg.translatedText ?: "",
                    sourceLang = msg.sourceLang,
                    targetLang = msg.targetLang,
                    direction = if (msg.isLocal) "local" else "remote",
                    translationMode = msg.mode?.name?.lowercase() ?: "unknown",
                    latencyMs = latencyMs,
                    createdAt = msg.timestamp
                )
            )
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Failed to save translation", e)
        }
    }

    private suspend fun handleIncomingRequest(request: TranslateRequestDto) {
        try {
            val engine = selectEngine()
            val result = engine.translate(
                TranslationRequest(request.text, request.sourceLang, request.targetLang)
            )
            val msg = ChatMessage(
                id = request.requestId,
                text = request.text,
                translatedText = result.translatedText,
                senderAlias = request.senderAlias,
                senderId = request.senderId,
                isLocal = false,
                sourceLang = request.sourceLang,
                targetLang = request.targetLang,
                mode = result.mode
            )
            _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
            saveTranslation(msg, result.latencyMs)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Handle incoming request failed", e)
        }
    }

    private fun handleIncomingResult(result: TranslateResultDto) {
        try {
            val msg = ChatMessage(
                id = result.requestId,
                text = result.originalText,
                translatedText = result.translatedText,
                senderAlias = "Remote",
                senderId = result.senderId,
                isLocal = false,
                sourceLang = result.sourceLang,
                targetLang = result.targetLang,
                mode = try { TranslationMode.valueOf(result.translationMode.uppercase()) } catch (_: Exception) { null }
            )
            _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + msg)
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Handle incoming result failed", e)
        }
    }

    private fun updateMessage(msg: ChatMessage) {
        val messages = _uiState.value.messages.toMutableList()
        val idx = messages.indexOfFirst { it.id == msg.id }
        if (idx >= 0) {
            messages[idx] = msg
            _uiState.value = _uiState.value.copy(messages = messages)
        }
    }

    private fun buildMulticastDto() = MulticastDto(
        alias = deviceAlias,
        deviceModel = android.os.Build.MODEL ?: "Unknown",
        fingerprint = fingerprint,
        port = DeviceInfo.DEFAULT_PORT,
        announcement = true
    )

    override fun onCleared() {
        try {
            httpServer?.stop()
            multicastDiscovery?.stop()
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error stopping services", e)
        }
        super.onCleared()
    }
}
