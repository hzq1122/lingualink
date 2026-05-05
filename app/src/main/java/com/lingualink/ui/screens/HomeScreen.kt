package com.lingualink.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lingualink.domain.model.ConnectionStatus
import com.lingualink.domain.model.DeviceInfo
import com.lingualink.domain.model.TranslationMode
import com.lingualink.ui.components.LanguageSelector
import com.lingualink.ui.viewmodel.ChatMessage
import com.lingualink.ui.viewmodel.DeviceConnection
import com.lingualink.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Top bar
            Surface(
                shadowElevation = 2.dp,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LanguageSelector(
                            selectedCode = state.sourceLang,
                            onSelected = { viewModel.setSourceLang(it) },
                            modifier = Modifier.weight(1f)
                        )
                        FilledIconButton(
                            onClick = { viewModel.swapLanguages() },
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.SwapHoriz, "交换语言", Modifier.size(20.dp))
                        }
                        LanguageSelector(
                            selectedCode = state.targetLang,
                            onSelected = { viewModel.setTargetLang(it) },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(4.dp))
                        FilledTonalIconButton(
                            onClick = { viewModel.startScan() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (state.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, "扫描设备", Modifier.size(20.dp))
                            }
                        }
                    }

                    DeviceStatusBar(
                        isServerRunning = state.isServerRunning,
                        connectedDevices = state.connectedDevices,
                        discoveredDevices = state.discoveredDevices,
                        isScanning = state.isScanning,
                        onConnect = { viewModel.connectDevice(it) },
                        onDisconnect = { viewModel.disconnectDevice(it) }
                    )
                }
            }

            // Messages
            if (state.messages.isEmpty()) {
                EmptyHomeState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(state.messages, key = { it.id }) { msg ->
                        MessageBubble(msg)
                    }
                }
            }

            // Input bar
            Surface(tonalElevation = 3.dp, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入文本...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            viewModel.sendTranslation(inputText)
                            inputText = ""
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "发送", Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceStatusBar(
    isServerRunning: Boolean,
    connectedDevices: List<DeviceConnection>,
    discoveredDevices: List<DeviceInfo>,
    isScanning: Boolean,
    onConnect: (DeviceInfo) -> Unit,
    onDisconnect: (DeviceInfo) -> Unit
) {
    val connectedCount = connectedDevices.count { it.status == ConnectionStatus.CONNECTED }
    val hasConnected = connectedCount > 0
    val hasConnecting = connectedDevices.any { it.status == ConnectionStatus.CONNECTING }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasConnected -> MaterialTheme.colorScheme.primary
                            hasConnecting -> MaterialTheme.colorScheme.tertiary
                            isScanning -> MaterialTheme.colorScheme.tertiary
                            isServerRunning -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    isScanning -> "正在扫描附近设备..."
                    hasConnecting -> "正在连接设备..."
                    hasConnected -> "已连接 $connectedCount 台设备"
                    isServerRunning -> "服务就绪 · 等待连接"
                    else -> "服务未启动"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (hasConnected || hasConnecting) FontWeight.Medium else FontWeight.Normal
            )
        }

        if (connectedDevices.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(connectedDevices, key = { it.device.fingerprint }) { conn ->
                    val isConnecting = conn.status == ConnectionStatus.CONNECTING
                    AssistChip(
                        onClick = { onDisconnect(conn.device) },
                        label = {
                            Text(
                                if (isConnecting) "${conn.device.alias} · 连接中"
                                else conn.device.alias,
                                fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Icon(Icons.Default.Link, null, Modifier.size(14.dp))
                            }
                        },
                        trailingIcon = { Icon(Icons.Default.Close, "断开", Modifier.size(14.dp)) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        if (discoveredDevices.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "发现设备",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "点击连接",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(discoveredDevices, key = { it.fingerprint }) { device ->
                    AssistChip(
                        onClick = { onConnect(device) },
                        label = { Text(device.alias, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Default.Add, "连接", Modifier.size(14.dp)) },
                        shape = RoundedCornerShape(20.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        AnimatedVisibility(visible = isScanning, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun EmptyHomeState(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Translate,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "LinguaLink",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "输入文本直接翻译",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "点击扫描按钮发现附近设备",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val isLocal = msg.isLocal
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isLocal) 16.dp else 4.dp,
        bottomEnd = if (isLocal) 4.dp else 16.dp
    )

    Column(
        horizontalAlignment = if (isLocal) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            msg.senderAlias,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(bubbleShape)
                .background(
                    if (isLocal) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(msg.text, fontSize = 15.sp, lineHeight = 22.sp)
        }

        if (msg.isTranslating) {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                Spacer(Modifier.width(6.dp))
                Text("翻译中...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        msg.translatedText?.let { translated ->
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(translated, fontSize = 15.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        msg.mode?.let { mode ->
                            if (mode == TranslationMode.OFFLINE) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.height(16.dp)
                                ) {
                                    Text(
                                        "离线",
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                        Text(
                            "${msg.sourceLang} -> ${msg.targetLang}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
