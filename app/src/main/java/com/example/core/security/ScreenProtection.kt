package com.example.core.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Applies Android FLAG_SECURE on sensitive/paid screens to block screenshots,
 * screen recording, and obscure content in the Recent Apps switcher.
 */
@Composable
fun SecureScreenEffect(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(context, enabled) {
        val activity = context.findActivity()
        val window = activity?.window
        if (window != null && enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.setRecentsScreenshotEnabled(false)
            }
        }
        onDispose {
            if (window != null && enabled) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    activity.setRecentsScreenshotEnabled(true)
                }
            }
        }
    }
}

fun shareTextDocument(context: Context, subject: String, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, subject))
}

fun openExternalUrlSafe(context: Context, rawUrl: String): Boolean {
    val trimmed = rawUrl.trim()
    if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(trimmed))
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
