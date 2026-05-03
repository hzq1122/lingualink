package com.lingualink.ui.viewmodel

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    private val offlineEngine: OfflineTranslationEngine
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
        _uiState.value = _uiState.value.copy(isScanning = true)
        val discovery = MulticastDiscovery(
            deviceClient = deviceClient,
            scope = viewModelScope
        )
        multicastDiscovery = discovery

        viewModelScope.launch {
            discovery.discovered.collect { device ->
                val current = _uiState.value.discoveredDevices.toMutableList()
                if (current.none { it.fingerprint == device.fingerprint }) {
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
            targetLang = state.targetLang
        )
        _uiState.value = state.copy(messages = state.messages + localMsg)

        viewModelScope.launch {
            try {
                val engine = if (onlineEngine.isAvailable()) onlineEngine else offlineEngine
                val result = engine.translate(
                    TranslationRequest(text, state.sourceLang, state.targetLang)
                )
                val updatedMsg = localMsg.copy(
                    translatedText = result.translatedText,
                    mode = result.mode
                )
                updateMessage(updatedMsg)

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
                _uiState.value = _uiState.value.copy(errorMessage = "翻译失败: ${e.message}")
            }
        }
    }

    private suspend fun handleIncomingRequest(request: TranslateRequestDto) {
        try {
            val engine = if (onlineEngine.isAvailable()) onlineEngine else offlineEngine
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
                mode = TranslationMode.valueOf(result.translationMode.uppercase())
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
