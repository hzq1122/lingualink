package com.lingualink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lingualink.domain.model.TranslationMode
import com.lingualink.translation.SupportedLanguage
import com.lingualink.ui.components.LanguageSelector
import com.lingualink.ui.viewmodel.ChatMessage
import com.lingualink.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with language selector
        Surface(shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LanguageSelector(
                        selectedCode = state.sourceLang,
                        onSelected = { viewModel.setSourceLang(it) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.swapLanguages() }) {
                        Icon(Icons.Default.SwapHoriz, "交换语言")
                    }
                    LanguageSelector(
                        selectedCode = state.targetLang,
                        onSelected = { viewModel.setTargetLang(it) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.startScan() }) {
                        Icon(Icons.Default.Search, "扫描设备")
                    }
                }

                // Discovered devices
                if (state.discoveredDevices.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.discoveredDevices.forEach { device ->
                            AssistChip(
                                onClick = {},
                                label = { Text(device.alias, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                MessageBubble(msg)
            }
        }

        // Error
        state.errorMessage?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp), fontSize = 12.sp)
        }

        // Input
        Surface(shadowElevation = 8.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入文本...") },
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.sendTranslation(inputText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "发送")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val alignment = if (msg.isLocal) Alignment.End else Alignment.Start
    val bubbleColor = if (msg.isLocal)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Sender name
        Text(
            msg.senderAlias,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Original text
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(msg.text, fontSize = 15.sp)
        }

        // Translated text
        msg.translatedText?.let { translated ->
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(12.dp)
            ) {
                Column {
                    Text(translated, fontSize = 15.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        msg.mode?.let { mode ->
                            if (mode == TranslationMode.OFFLINE) {
                                Text(
                                    "离线", fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                        Text(
                            "${msg.sourceLang} → ${msg.targetLang}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
