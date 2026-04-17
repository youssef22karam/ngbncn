package com.jarvis.ai.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.jarvis.ai.services.JarvisAccessibilityService
import com.jarvis.ai.ui.components.*
import com.jarvis.ai.ui.theme.*
import com.jarvis.ai.viewmodel.MainViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val accessEnabled by viewModel.accessibilityEnabled.collectAsStateWithLifecycle()

    val micPermission     = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val notifPermission   = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    val cameraPermission  = rememberPermissionState(Manifest.permission.CAMERA)

    val overlayGranted = Settings.canDrawOverlays(context)

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(JarvisBackground, Color(0xFF060D18))))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = JarvisBlue)
            }
            Column {
                GlowText("PERMISSIONS", fontSize = 20.sp, letterSpacing = 6.sp)
                Text("Enable JARVIS capabilities",
                    style = MaterialTheme.typography.labelSmall,
                    color = JarvisTextSub, letterSpacing = 1.sp)
            }
        }
        Divider(color = JarvisDivider, thickness = 0.5.dp)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Grant these permissions to unlock JARVIS's full capabilities. " +
                        "Core features work with microphone only.",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextSub, lineHeight = 20.sp
            )

            // Microphone — REQUIRED
            PermissionCard(
                icon       = Icons.Filled.Mic,
                title      = "Microphone",
                desc       = "Required for voice commands and speech recognition",
                badge      = "REQUIRED",
                badgeColor = JarvisRed,
                granted    = micPermission.status.isGranted,
                onGrant    = { micPermission.launchPermissionRequest() }
            )

            // Notifications
            PermissionCard(
                icon       = Icons.Filled.Notifications,
                title      = "Notifications",
                desc       = "Show JARVIS status and controls in notification bar",
                badge      = "RECOMMENDED",
                badgeColor = JarvisWarning,
                granted    = notifPermission.status.isGranted,
                onGrant    = { notifPermission.launchPermissionRequest() }
            )

            // Accessibility — PHONE CONTROL
            PermissionCard(
                icon       = Icons.Filled.Accessibility,
                title      = "Accessibility Service",
                desc       = "Allows JARVIS to control your phone: tap buttons, open apps, type text, scroll, and read screen content",
                badge      = "PHONE CONTROL",
                badgeColor = JarvisBlue,
                granted    = accessEnabled,
                grantLabel = if (accessEnabled) "Granted" else "Open Settings",
                onGrant    = {
                    if (!accessEnabled) {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                },
                extraContent = if (!accessEnabled) {
                    {
                        HudCard {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("How to enable:", style = MaterialTheme.typography.bodyMedium,
                                    color = JarvisBlue, fontWeight = FontWeight.Medium)
                                Text("1. Tap 'Open Settings' below\n2. Find 'Downloaded Apps' or 'Installed Services'\n3. Tap 'JARVIS AI' → Enable",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = JarvisTextSub, fontSize = 12.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                } else null
            )

            // Overlay (floating JARVIS)
            PermissionCard(
                icon       = Icons.Filled.Layers,
                title      = "Display Over Other Apps",
                desc       = "Show floating JARVIS orb while using other apps",
                badge      = "OPTIONAL",
                badgeColor = JarvisTextSub,
                granted    = overlayGranted,
                grantLabel = if (overlayGranted) "Granted" else "Open Settings",
                onGrant    = {
                    if (!overlayGranted) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                }
            )

            // Camera
            PermissionCard(
                icon       = Icons.Filled.Camera,
                title      = "Camera",
                desc       = "Use camera for visual context (upcoming feature)",
                badge      = "OPTIONAL",
                badgeColor = JarvisTextSub,
                granted    = cameraPermission.status.isGranted,
                onGrant    = { cameraPermission.launchPermissionRequest() }
            )

            // Overall status
            val allCore = micPermission.status.isGranted
            val allPower = micPermission.status.isGranted && accessEnabled && overlayGranted

            HudCard(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (allPower) Icons.Filled.VerifiedUser else Icons.Filled.Shield,
                        null,
                        tint = if (allPower) JarvisGreen else if (allCore) JarvisWarning else JarvisRed,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            when {
                                allPower -> "Full Power Mode"
                                allCore  -> "Basic Mode"
                                else     -> "Setup Required"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisText, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            when {
                                allPower -> "JARVIS has all capabilities enabled"
                                allCore  -> "Voice works. Grant accessibility for phone control"
                                else     -> "Microphone permission is needed to start"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextSub, fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    desc: String,
    badge: String,
    badgeColor: Color,
    granted: Boolean,
    grantLabel: String = "Grant",
    onGrant: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisCardBg)
            .border(
                if (granted) 1.dp else 0.5.dp,
                if (granted) JarvisGreen.copy(alpha = 0.5f) else JarvisDivider,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(
                        if (granted) JarvisGreen.copy(alpha = 0.1f) else JarvisNavy,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null,
                    tint = if (granted) JarvisGreen else JarvisBlue,
                    modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium,
                        color = JarvisText, fontWeight = FontWeight.Medium)
                    StatusBadge(badge, badgeColor)
                }
                Text(desc, style = MaterialTheme.typography.bodyMedium,
                    color = JarvisTextSub, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }

        extraContent?.invoke()

        Button(
            onClick  = onGrant,
            enabled  = !granted,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (granted) JarvisGreen.copy(alpha = 0.15f) else JarvisBlue.copy(alpha = 0.15f),
                contentColor   = if (granted) JarvisGreen else JarvisBlue,
                disabledContainerColor = JarvisGreen.copy(alpha = 0.1f),
                disabledContentColor   = JarvisGreen
            ),
            border = BorderStroke(1.dp, if (granted) JarvisGreen.copy(alpha = 0.4f) else JarvisBlue.copy(alpha = 0.3f)),
            shape  = RoundedCornerShape(10.dp)
        ) {
            Icon(
                if (granted) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                null, Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(if (granted) "✓ Granted" else grantLabel, fontWeight = FontWeight.Medium)
        }
    }
}
