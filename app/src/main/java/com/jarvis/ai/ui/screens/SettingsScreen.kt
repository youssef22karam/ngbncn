package com.jarvis.ai.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jarvis.ai.ui.components.*
import com.jarvis.ai.ui.theme.*
import com.jarvis.ai.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val ttsSpeed      by viewModel.ttsSpeed.collectAsStateWithLifecycle()
    val ttsPitch      by viewModel.ttsPitch.collectAsStateWithLifecycle()
    val wakeWord      by viewModel.wakeWordEnabled.collectAsStateWithLifecycle()
    val systemPrompt  by viewModel.systemPrompt.collectAsStateWithLifecycle()
    val temperature   by viewModel.llmTemperature.collectAsStateWithLifecycle()
    val maxTokens     by viewModel.llmMaxTokens.collectAsStateWithLifecycle()
    val jarvisName    by viewModel.jarvisName.collectAsStateWithLifecycle()

    var editingPrompt by remember { mutableStateOf(false) }
    var promptDraft   by remember(systemPrompt) { mutableStateOf(systemPrompt) }
    var nameDraft     by remember(jarvisName) { mutableStateOf(jarvisName) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(JarvisBackground, Color(0xFF060D18))))
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = JarvisBlue)
            }
            GlowText("SETTINGS", fontSize = 20.sp, letterSpacing = 6.sp)
        }
        Divider(color = JarvisDivider, thickness = 0.5.dp)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === Assistant Identity ===
            SettingsSection("ASSISTANT IDENTITY", Icons.Filled.Person) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Assistant Name", style = MaterialTheme.typography.bodyMedium,
                        color = JarvisTextSub)
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = {
                            nameDraft = it
                            viewModel.setJarvisName(it)
                        },
                        modifier    = Modifier.fillMaxWidth(),
                        singleLine  = true,
                        colors = jarvisTextFieldColors(),
                        shape  = RoundedCornerShape(10.dp)
                    )
                }
            }

            // === Voice ===
            SettingsSection("VOICE OUTPUT", Icons.Filled.RecordVoiceOver) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SliderSetting(
                        label      = "Speech Speed",
                        value      = ttsSpeed,
                        range      = 0.5f..2.0f,
                        steps      = 14,
                        displayVal = "%.1fx".format(ttsSpeed),
                        onChange   = viewModel::setTtsSpeed
                    )
                    SliderSetting(
                        label      = "Voice Pitch",
                        value      = ttsPitch,
                        range      = 0.5f..1.5f,
                        steps      = 9,
                        displayVal = "%.1f".format(ttsPitch),
                        onChange   = viewModel::setTtsPitch
                    )
                }
            }

            // === Speech Recognition ===
            SettingsSection("SPEECH INPUT", Icons.Filled.Mic) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SwitchSetting(
                        label    = "Wake Word Mode",
                        subLabel = "Say 'Hey JARVIS' to activate",
                        checked  = wakeWord,
                        onChange = viewModel::setWakeWordEnabled,
                        icon     = Icons.Filled.Hearing
                    )
                }
            }

            // === LLM ===
            SettingsSection("AI MODEL PARAMETERS", Icons.Filled.Psychology) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SliderSetting(
                        label      = "Creativity (Temperature)",
                        value      = temperature,
                        range      = 0.0f..1.5f,
                        steps      = 14,
                        displayVal = "%.1f".format(temperature),
                        onChange   = viewModel::setLlmTemperature
                    )
                    SliderSetting(
                        label      = "Max Response Tokens",
                        value      = maxTokens.toFloat(),
                        range      = 64f..2048f,
                        steps      = 30,
                        displayVal = "$maxTokens",
                        onChange   = { viewModel.setLlmMaxTokens(it.toInt()) }
                    )
                }
            }

            // === System Prompt ===
            SettingsSection("SYSTEM PROMPT", Icons.Filled.Code) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Define JARVIS's personality and capabilities",
                        style = MaterialTheme.typography.bodyMedium, color = JarvisTextSub
                    )
                    OutlinedTextField(
                        value         = promptDraft,
                        onValueChange = { promptDraft = it },
                        modifier      = Modifier.fillMaxWidth().height(160.dp),
                        colors        = jarvisTextFieldColors(),
                        shape         = RoundedCornerShape(10.dp),
                        textStyle     = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    Button(
                        onClick = { viewModel.setSystemPrompt(promptDraft) },
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = JarvisBlue.copy(alpha = 0.2f),
                            contentColor   = JarvisBlue
                        ),
                        border = BorderStroke(1.dp, JarvisBlue.copy(alpha = 0.5f)),
                        shape  = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Save, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Prompt")
                    }
                }
            }

            // === Chat ===
            SettingsSection("CONVERSATION", Icons.Filled.Chat) {
                Button(
                    onClick  = { viewModel.clearChat() },
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = JarvisRed.copy(alpha = 0.1f),
                        contentColor   = JarvisRed
                    ),
                    border = BorderStroke(1.dp, JarvisRed.copy(alpha = 0.4f)),
                    shape  = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.DeleteSweep, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear Chat History")
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisCardBg)
            .border(0.5.dp, JarvisDivider, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = JarvisBlue, modifier = Modifier.size(16.dp))
            Text(
                text       = title,
                style      = MaterialTheme.typography.labelSmall,
                color      = JarvisBlue,
                letterSpacing = 1.5.sp,
                fontSize   = 10.sp
            )
        }
        Divider(color = JarvisDivider, thickness = 0.5.dp)
        content()
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayVal: String,
    onChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = JarvisText)
            Text(displayVal, style = MaterialTheme.typography.bodyMedium,
                color = JarvisBlue, fontWeight = FontWeight.Medium)
        }
        Slider(
            value          = value,
            onValueChange  = onChange,
            valueRange     = range,
            steps          = steps,
            colors         = SliderDefaults.colors(
                thumbColor       = JarvisBlue,
                activeTrackColor = JarvisBlue,
                inactiveTrackColor = JarvisDivider
            )
        )
    }
}

@Composable
private fun SwitchSetting(
    label: String,
    subLabel: String = "",
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    icon: ImageVector? = null
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            icon?.let {
                Icon(it, null, tint = JarvisBlue.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = JarvisText)
                if (subLabel.isNotBlank()) {
                    Text(subLabel, style = MaterialTheme.typography.bodyMedium,
                        color = JarvisTextSub, fontSize = 11.sp)
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = JarvisBackground,
                checkedTrackColor  = JarvisBlue,
                uncheckedThumbColor = JarvisTextSub,
                uncheckedTrackColor = JarvisDivider
            )
        )
    }
}

@Composable
private fun jarvisTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = JarvisBlue,
    unfocusedBorderColor = JarvisDivider,
    focusedTextColor     = JarvisText,
    unfocusedTextColor   = JarvisText,
    cursorColor          = JarvisBlue,
    focusedLabelColor    = JarvisBlue,
    unfocusedLabelColor  = JarvisTextSub
)
