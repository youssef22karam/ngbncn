package com.jarvis.ai.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.jarvis.ai.data.ChatMessage
import com.jarvis.ai.ui.components.*
import com.jarvis.ai.ui.theme.*
import com.jarvis.ai.viewmodel.MainViewModel
import com.jarvis.ai.viewmodel.ModelLoadState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // State
    val messages         by viewModel.messages.collectAsStateWithLifecycle()
    val isListening      by viewModel.isListening.collectAsStateWithLifecycle()
    val isThinking       by viewModel.isThinking.collectAsStateWithLifecycle()
    val isSpeaking       by viewModel.isSpeakingTTS.collectAsStateWithLifecycle()
    val partialText      by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val modelState       by viewModel.modelLoadState.collectAsStateWithLifecycle()
    val activeModel      by viewModel.activeModelInfo.collectAsStateWithLifecycle()
    val amplitude        by viewModel.micAmplitude.collectAsStateWithLifecycle()
    val jarvisName       by viewModel.jarvisName.collectAsStateWithLifecycle()
    val autoListen       by viewModel.autoListen.collectAsStateWithLifecycle()
    val accessEnabled    by viewModel.accessibilityEnabled.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }

    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Start background service
    LaunchedEffect(Unit) {
        viewModel.startBackgroundService(context)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(JarvisBackground, Color(0xFF060D18))
                )
            )
    ) {
        // === Grid background pattern ===
        Canvas(Modifier.fillMaxSize()) {
            val step = 40.dp.toPx()
            val lineColor = JarvisDivider.copy(alpha = 0.3f)
            var x = 0f
            while (x <= size.width) {
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 0.5f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 0.5f)
                y += step
            }
        }

        Column(Modifier.fillMaxSize()) {
            // ── Top Bar ──────────────────────────────────────────────────────
            TopBar(
                jarvisName      = jarvisName,
                modelState      = modelState,
                activeModelName = activeModel?.name,
                accessEnabled   = accessEnabled,
                onModels        = { navController.navigate(Screen.Models.route) },
                onSettings      = { navController.navigate(Screen.Settings.route) },
                onPermissions   = { navController.navigate(Screen.Permissions.route) }
            )

            // ── Chat Messages ────────────────────────────────────────────────
            LazyColumn(
                state              = listState,
                modifier           = Modifier.weight(1f),
                contentPadding     = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item { WelcomePanel(jarvisName = jarvisName, modelState = modelState,
                        onGoToModels = { navController.navigate(Screen.Models.route) }) }
                }
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg)
                }
                // Partial transcript while listening
                if (partialText.isNotBlank()) {
                    item {
                        Text(
                            text   = "🎤 $partialText",
                            style  = MaterialTheme.typography.bodyMedium,
                            color  = JarvisBlue.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // ── Voice Waveform (while listening) ─────────────────────────────
            AnimatedVisibility(
                visible = isListening,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    VoiceWaveform(
                        amplitude = amplitude,
                        isActive  = isListening,
                        modifier  = Modifier
                            .height(48.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
            }

            // ── Text Input ───────────────────────────────────────────────────
            AnimatedVisibility(
                visible = showTextInput,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value         = textInput,
                        onValueChange = { textInput = it },
                        modifier      = Modifier.weight(1f),
                        placeholder   = { Text("Type a command...", color = JarvisTextSub) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = JarvisBlue,
                            unfocusedBorderColor = JarvisDivider,
                            focusedTextColor     = JarvisText,
                            unfocusedTextColor   = JarvisText,
                            cursorColor          = JarvisBlue
                        ),
                        shape    = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput.trim())
                                textInput = ""
                                showTextInput = false
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Send, "Send", tint = JarvisBlue)
                    }
                }
            }

            // ── Bottom Controls ──────────────────────────────────────────────
            BottomControls(
                isListening    = isListening,
                isThinking     = isThinking,
                isSpeaking     = isSpeaking,
                autoListen     = autoListen,
                showTextInput  = showTextInput,
                amplitude      = amplitude,
                onMicClick     = {
                    if (!micPermission.status.isGranted) {
                        micPermission.launchPermissionRequest()
                    } else if (isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                },
                onStopSpeak    = { viewModel.stopSpeaking() },
                onToggleText   = { showTextInput = !showTextInput },
                onToggleAuto   = { viewModel.toggleContinuousListen(!autoListen) },
                onClearChat    = { viewModel.clearChat() }
            )
        }
    }
}

@Composable
private fun TopBar(
    jarvisName: String,
    modelState: ModelLoadState,
    activeModelName: String?,
    accessEnabled: Boolean,
    onModels: () -> Unit,
    onSettings: () -> Unit,
    onPermissions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisBackground.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            GlowText(text = jarvisName, fontSize = 22.sp, letterSpacing = 8.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusLabel, statusColor) = when (modelState) {
                    is ModelLoadState.Ready   -> "ONLINE" to JarvisGreen
                    is ModelLoadState.Loading -> "LOADING" to JarvisWarning
                    is ModelLoadState.Error   -> "ERROR"  to JarvisRed
                    is ModelLoadState.Idle    -> "OFFLINE" to JarvisTextSub
                }
                Box(
                    Modifier
                        .size(6.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text  = activeModelName ?: statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
                if (!accessEnabled) {
                    StatusBadge("NO CONTROL", JarvisWarning)
                }
            }
        }

        // Action buttons
        IconButton(onClick = onModels) {
            Icon(Icons.Filled.Download, "Models", tint = JarvisBlue)
        }
        IconButton(onClick = onPermissions) {
            Icon(Icons.Filled.AdminPanelSettings, "Permissions",
                tint = if (accessEnabled) JarvisGreen else JarvisWarning)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, "Settings", tint = JarvisTextSub)
        }
    }
    Divider(color = JarvisDivider, thickness = 0.5.dp)
}

@Composable
private fun WelcomePanel(
    jarvisName: String,
    modelState: ModelLoadState,
    onGoToModels: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PulseRing(isActive = modelState is ModelLoadState.Ready, amplitude = 0f)

        GlowText(text = "J.A.R.V.I.S", fontSize = 32.sp, letterSpacing = 12.sp)
        Text(
            text  = "Just A Rather Very Intelligent System",
            style = MaterialTheme.typography.bodyMedium,
            color = JarvisTextSub,
            letterSpacing = 2.sp,
            fontSize = 10.sp
        )

        when (modelState) {
            is ModelLoadState.Idle -> {
                HudCard(Modifier.padding(horizontal = 24.dp)) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "No model loaded",
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisText
                        )
                        Text(
                            "Download and activate an AI model to start",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextSub,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onGoToModels,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisBlue,
                                contentColor   = JarvisBackground
                            )
                        ) {
                            Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Get Models", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            is ModelLoadState.Loading -> {
                HudCard(Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = JarvisBlue,
                            strokeWidth = 2.dp
                        )
                        Text("Loading ${modelState.name}...", color = JarvisText)
                    }
                }
            }
            is ModelLoadState.Ready -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge("SYSTEM ONLINE", JarvisGreen)
                    Text(
                        "Press and hold the mic to speak, or tap for\na single command",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = JarvisTextSub,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is ModelLoadState.Error -> {
                StatusBadge("ERROR: ${modelState.message}", JarvisRed)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(JarvisNavy, CircleShape)
                    .border(1.dp, JarvisBlue.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("J", fontSize = 12.sp, color = JarvisBlue, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (isUser) JarvisBlue.copy(alpha = 0.15f) else JarvisCardBg,
                    RoundedCornerShape(
                        topStart    = if (isUser) 16.dp else 4.dp,
                        topEnd      = if (isUser) 4.dp  else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd   = 16.dp
                    )
                )
                .border(
                    0.5.dp,
                    if (isUser) JarvisBlue.copy(alpha = 0.3f) else JarvisDivider,
                    RoundedCornerShape(
                        topStart    = if (isUser) 16.dp else 4.dp,
                        topEnd      = if (isUser) 4.dp  else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd   = 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (message.isThinking && message.content.isBlank()) {
                ThinkingDots()
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (message.isVoice && isUser) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Mic,
                                null,
                                tint     = JarvisBlue.copy(alpha = 0.6f),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                "Voice",
                                fontSize  = 9.sp,
                                color     = JarvisBlue.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    Text(
                        text  = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) JarvisText else JarvisText.copy(alpha = 0.9f)
                    )
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(28.dp)
                    .background(JarvisBlue.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, JarvisBlue.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person, null,
                    tint     = JarvisBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomControls(
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    autoListen: Boolean,
    showTextInput: Boolean,
    amplitude: Float,
    onMicClick: () -> Unit,
    onStopSpeak: () -> Unit,
    onToggleText: () -> Unit,
    onToggleAuto: () -> Unit,
    onClearChat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisBackground.copy(alpha = 0.95f))
            .padding(bottom = 24.dp, top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(color = JarvisDivider, thickness = 0.5.dp)
        // Status text
        Text(
            text = when {
                isListening -> "LISTENING..."
                isThinking  -> "PROCESSING..."
                isSpeaking  -> "SPEAKING..."
                autoListen  -> "WAKE WORD ACTIVE"
                else        -> "TAP MIC TO SPEAK"
            },
            style     = MaterialTheme.typography.labelSmall,
            color     = when {
                isListening -> JarvisBlue
                isThinking  -> JarvisOrange
                isSpeaking  -> JarvisGreen
                autoListen  -> JarvisWarning
                else        -> JarvisTextSub
            },
            letterSpacing = 2.sp,
            fontSize = 9.sp
        )

        // Main controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text toggle
            IconButton(onClick = onToggleText) {
                Icon(
                    if (showTextInput) Icons.Filled.KeyboardHide else Icons.Filled.Keyboard,
                    "Text",
                    tint = if (showTextInput) JarvisBlue else JarvisTextSub,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Big mic button
            Box(contentAlignment = Alignment.Center) {
                if (isListening) {
                    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue  = 1.2f + amplitude * 0.4f,
                        animationSpec = infiniteRepeatable(
                            animation  = tween(300, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ), label = "mic_scale"
                    )
                    Box(
                        Modifier
                            .size(80.dp)
                            .scale(scale)
                            .alpha(0.25f)
                            .background(JarvisBlue, CircleShape)
                    )
                }
                FloatingActionButton(
                    onClick            = onMicClick,
                    modifier           = Modifier.size(64.dp),
                    containerColor     = when {
                        isListening -> JarvisBlue
                        isThinking  -> JarvisOrange
                        else        -> JarvisNavy
                    },
                    contentColor       = if (isListening) JarvisBackground else JarvisBlue,
                    shape              = CircleShape,
                    elevation          = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    when {
                        isListening -> Icon(Icons.Filled.MicOff, "Stop",  Modifier.size(28.dp))
                        isThinking  -> CircularProgressIndicator(
                            modifier    = Modifier.size(28.dp),
                            color       = JarvisBackground,
                            strokeWidth = 2.dp
                        )
                        else        -> Icon(Icons.Filled.Mic, "Speak", Modifier.size(28.dp))
                    }
                }
            }

            // Stop speaking / auto-listen toggle
            if (isSpeaking) {
                IconButton(onClick = onStopSpeak) {
                    Icon(Icons.Filled.VolumeOff, "Stop", tint = JarvisOrange, modifier = Modifier.size(22.dp))
                }
            } else {
                IconButton(onClick = onToggleAuto) {
                    Icon(
                        if (autoListen) Icons.Filled.HearingDisabled else Icons.Filled.Hearing,
                        "Auto",
                        tint     = if (autoListen) JarvisWarning else JarvisTextSub,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
