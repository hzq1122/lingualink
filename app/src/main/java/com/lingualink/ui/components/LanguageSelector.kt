package com.lingualink.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lingualink.translation.SupportedLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    selectedCode: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = SupportedLanguage.fromCode(selectedCode)
    val languages = SupportedLanguage.onlineLanguages()
    val sorted = languages.sortedByDescending { it.code in SupportedLanguage.priorityCodes }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.displayName ?: selectedCode,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().width(120.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sorted.forEach { lang ->
                DropdownMenuItem(
                    text = { Text("${lang.localName} (${lang.code})") },
                    onClick = {
                        onSelected(lang.code)
                        expanded = false
                    }
                )
            }
        }
    }
}
