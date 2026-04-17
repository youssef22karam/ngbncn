package com.jarvis.ai.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
        _isConnected.value = true
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        // Track current screen content for context
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString() ?: ""
                val cls = event.className?.toString() ?: ""
                _currentApp.value = pkg
                Log.d(TAG, "App changed: $pkg / $cls")
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // Could feed input changes to JARVIS context
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isConnected.value = false
    }

    // ── Phone Control Actions ───────────────────────────────────────────────

    /** Click the first node matching the content description or text */
    fun clickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByText(root, text) ?: run {
            Log.w(TAG, "Node not found: $text")
            return false
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK).also {
            node.recycle()
        }
    }

    /** Click at absolute screen coordinates */
    fun clickAt(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    /** Scroll the screen up or down */
    fun scroll(direction: String): Boolean {
        val bounds = Rect()
        rootInActiveWindow?.getBoundsInScreen(bounds)
        val cx = bounds.centerX().toFloat()
        val cy = bounds.centerY().toFloat()

        val (startY, endY) = when (direction.uppercase()) {
            "UP"   -> Pair(cy + 400, cy - 400)
            "DOWN" -> Pair(cy - 400, cy + 400)
            else   -> Pair(cy, cy)
        }

        val path = Path().apply {
            moveTo(cx, startY)
            lineTo(cx, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    /** Type text into the focused input */
    fun typeText(text: String): Boolean {
        val node = findFocusedEditText(rootInActiveWindow) ?: return false
        val args = Bundle().apply {
            putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            .also { node.recycle() }
    }

    /** Press Back */
    fun pressBack(): Boolean =
        performGlobalAction(GLOBAL_ACTION_BACK)

    /** Press Home */
    fun pressHome(): Boolean =
        performGlobalAction(GLOBAL_ACTION_HOME)

    /** Open recent apps */
    fun openRecentApps(): Boolean =
        performGlobalAction(GLOBAL_ACTION_RECENTS)

    /** Take screenshot via accessibility */
    fun takeAccessibilityScreenshot(): Boolean =
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)

    /** Open quick settings */
    fun openQuickSettings(): Boolean =
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

    /** Notifications panel */
    fun openNotifications(): Boolean =
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    /** Get all visible text on screen (for context) */
    fun getScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        return buildString {
            extractText(root, this)
        }.trim()
    }

    /** Open an app by its name (searches installed apps) */
    fun openApp(appName: String): Boolean {
        val pm = applicationContext.packageManager
        val apps = pm.getInstalledApplications(0)
        val target = apps.firstOrNull { app ->
            pm.getApplicationLabel(app).toString()
                .contains(appName, ignoreCase = true)
        } ?: return false

        val launchIntent = pm.getLaunchIntentForPackage(target.packageName)
            ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(launchIntent)
        return true
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        // Try exact text match first
        var found = root.findAccessibilityNodeInfosByText(text)?.firstOrNull()
        if (found != null) return found

        // Try content description
        found = root.findAccessibilityNodeInfosByViewId(text)?.firstOrNull()
        return found
    }

    private fun findFocusedEditText(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        root ?: return null
        if (root.isFocused && root.className?.contains("EditText") == true) return root
        for (i in 0 until root.childCount) {
            val result = findFocusedEditText(root.getChild(i))
            if (result != null) return result
        }
        return null
    }

    private fun extractText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        node ?: return
        node.text?.let { sb.appendLine(it) }
        node.contentDescription?.let { sb.appendLine(it) }
        for (i in 0 until node.childCount) {
            extractText(node.getChild(i), sb)
        }
    }

    companion object {
        private const val TAG = "JarvisAccessibility"
        var instance: JarvisAccessibilityService? = null
            private set

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected

        private val _currentApp = MutableStateFlow("")
        val currentApp: StateFlow<String> = _currentApp

        fun isEnabled() = instance != null
    }
}
