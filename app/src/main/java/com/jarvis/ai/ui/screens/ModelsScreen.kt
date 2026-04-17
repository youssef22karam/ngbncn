package com.jarvis.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jarvis.ai.data.*
import com.jarvis.ai.ui.components.*
import com.jarvis.ai.ui.theme.*
import com.jarvis.ai.viewmodel.MainViewModel

@Composable
fun ModelsScreen(viewModel: MainViewModel, navController: NavController) {
    val catalog       by viewModel.catalog.collectAsStateWithLifecycle()
    val downloads     by viewModel.downloadStates.collectAsStateWithLifecycle()
    val activeModel   by viewModel.activeModelInfo.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf<ModelInfo?>(null) }

    showDeleteDialog?.let { model ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            containerColor   = JarvisCardBg,
            title = {
                Text("Delete ${model.name}?", color = JarvisText)
            },
            text = {
                Text("This will remove the model file from storage. You can re-download it later.",
                    color = JarvisTextSub)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteModel(model)
                    showDeleteDialog = null
                }) {
                    Text("Delete", color = JarvisRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = JarvisTextSub)
                }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(JarvisBackground, Color(0xFF060D18))))
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = JarvisBlue)
            }
            Column(Modifier.weight(1f)) {
                GlowText("AI MODELS", fontSize = 20.sp, letterSpacing = 6.sp)
                Text("Download & Manage", style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSub, letterSpacing = 1.sp)
            }
        }
        Divider(color = JarvisDivider, thickness = 0.5.dp)

        // Storage info
        HudCard(Modifier.padding(16.dp).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Storage, null, tint = JarvisBlue, modifier = Modifier.size(20.dp))
                Column {
                    Text("Models stored in app private storage",
                        style = MaterialTheme.typography.bodyMedium, color = JarvisText)
                    Text("Models run fully offline after download",
                        style = MaterialTheme.typography.bodyMedium, color = JarvisTextSub,
                        fontSize = 11.sp)
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(catalog, key = { it.id }) { model ->
                val downloadState = downloads[model.id]
                ModelCard(
                    model        = model,
                    isActive     = activeModel?.id == model.id,
                    downloadState = downloadState,
                    onDownload   = { viewModel.downloadModel(model) },
                    onActivate   = { viewModel.activateModel(model) },
                    onDelete     = { showDeleteDialog = model }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    isActive: Boolean,
    downloadState: DownloadState?,
    onDownload: () -> Unit,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    val isDownloaded   = model.status == ModelStatus.DOWNLOADED
    val isDownloading  = model.status == ModelStatus.DOWNLOADING || downloadState != null
    val borderColor    = when {
        isActive      -> JarvisBlue
        isDownloaded  -> JarvisDivider.copy(alpha = 0.8f)
        else          -> JarvisDivider.copy(alpha = 0.4f)
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisCardBg)
            .border(
                if (isActive) 1.5.dp else 0.5.dp,
                if (isActive) Brush.linearGradient(listOf(JarvisBlue, JarvisDarkBlue))
                else Brush.linearGradient(listOf(borderColor, borderColor)),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Title row
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(model.iconEmoji, fontSize = 28.sp)
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(model.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisText, fontWeight = FontWeight.SemiBold)
                        if (model.badgeLabel.isNotBlank()) {
                            StatusBadge(
                                model.badgeLabel,
                                if (model.badgeLabel == "Recommended") JarvisGreen else JarvisBlue
                            )
                        }
                        if (isActive) StatusBadge("ACTIVE", JarvisBlue)
                    }
                    Text(model.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = JarvisTextSub, fontSize = 12.sp,
                        lineHeight = 18.sp)
                }
            }

            // Specs
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SpecChip("${model.parameters}", Icons.Filled.Memory)
                SpecChip(model.quantization, Icons.Filled.Compress)
                SpecChip(model.sizeLabel, Icons.Filled.Download)
                SpecChip("${model.contextLength / 1000}K ctx", Icons.Filled.TextFields)
            }

            // Download progress
            if (isDownloading && downloadState != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (downloadState.progress >= 0) "Downloading ${downloadState.progress}%"
                            else "Connecting...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisBlue, fontSize = 12.sp
                        )
                        Text(
                            "%.0f KB/s".format(downloadState.speedKbps),
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextSub, fontSize = 11.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = if (downloadState.progress >= 0) downloadState.progress / 100f else 0f,
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = JarvisBlue,
                        trackColor = JarvisDivider
                    )
                    if (downloadState.totalBytes > 0) {
                        Text(
                            "${formatBytes(downloadState.bytesDownloaded)} / ${formatBytes(downloadState.totalBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextSub, fontSize = 10.sp
                        )
                    }
                }
            }

            // Action buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    isDownloading -> {
                        OutlinedButton(
                            onClick  = {},
                            enabled  = false,
                            modifier = Modifier.weight(1f),
                            border   = BorderStroke(1.dp, JarvisDivider),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = JarvisBlue, strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Downloading...", color = JarvisTextSub, fontSize = 12.sp)
                        }
                    }
                    isDownloaded -> {
                        if (!isActive) {
                            Button(
                                onClick  = onActivate,
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = JarvisBlue,
                                    contentColor   = JarvisBackground
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Activate", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        } else {
                            Box(
                                Modifier.weight(1f).height(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(Modifier.size(6.dp).background(JarvisGreen, CircleShape))
                                    Text("Currently Active", color = JarvisGreen, fontSize = 12.sp)
                                }
                            }
                        }
                        IconButton(
                            onClick  = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Filled.Delete, "Delete",
                                tint = JarvisRed.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp))
                        }
                    }
                    else -> {
                        Button(
                            onClick  = onDownload,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = JarvisNavy,
                                contentColor   = JarvisBlue
                            ),
                            border = BorderStroke(1.dp, JarvisBlue.copy(alpha = 0.5f)),
                            shape  = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.CloudDownload, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Download ${model.sizeLabel}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .background(JarvisNavy, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Icon(icon, null, tint = JarvisBlue.copy(alpha = 0.7f), modifier = Modifier.size(10.dp))
        Text(label, fontSize = 10.sp, color = JarvisTextSub, letterSpacing = 0.5.sp)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576     -> "%.0f MB".format(bytes / 1_048_576.0)
    else                   -> "%.0f KB".format(bytes / 1_024.0)
}
