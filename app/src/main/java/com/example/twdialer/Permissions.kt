package com.example.twdialer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.twdialer.ui.TwColors

val DIALER_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.CALL_PHONE,
    Manifest.permission.READ_CALL_LOG,
)

data class PermissionState(val callPhone: Boolean, val contacts: Boolean, val callLog: Boolean) {
    companion object {
        fun read(c: Context) = PermissionState(
            callPhone = c.has(Manifest.permission.CALL_PHONE),
            contacts = c.has(Manifest.permission.READ_CONTACTS),
            callLog = c.has(Manifest.permission.READ_CALL_LOG),
        )
    }
}

private fun Context.has(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun DialerPermissionGate(content: @Composable (PermissionState) -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(PermissionState.read(context)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        state = PermissionState.read(context)
        val activity = context.findActivity()
        permanentlyDenied = activity != null && result.any { (perm, granted) ->
            !granted && !activity.shouldShowRequestPermissionRationale(perm)
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state = PermissionState.read(context) }

    LaunchedEffect(Unit) {
        if (!state.callPhone && !asked) {
            asked = true
            launcher.launch(DIALER_PERMISSIONS)
        }
    }

    if (state.callPhone) {
        content(state)
    } else {
        PermissionRationale(
            permanentlyDenied = permanentlyDenied,
            onRequest = { launcher.launch(DIALER_PERMISSIONS) },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            },
        )
    }
}

@Composable
private fun PermissionRationale(permanentlyDenied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(TwColors.HeaderTeal).safeDrawingPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Phone needs a few permissions", color = Color.White, fontSize = 22.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Phone access to place calls, Contacts to search by name with the keypad, and Call log to show recent calls.",
            color = TwColors.TabInactiveText, fontSize = 15.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = if (permanentlyDenied) onOpenSettings else onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = TwColors.Glow, contentColor = Color.White),
        ) { Text(if (permanentlyDenied) "Open settings" else "Grant permissions") }
    }
}

@SuppressLint("MissingPermission")
fun placeCall(context: Context, number: String) {
    if (number.isBlank()) return
    val telecom = context.getSystemService(TelecomManager::class.java) ?: return
    try {
        telecom.placeCall(Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null), Bundle())
    } catch (_: SecurityException) {
    }
}

fun createDialerRoleIntent(context: Context): Intent? {
    val rm = context.getSystemService(RoleManager::class.java) ?: return null
    return if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER))
        rm.createRequestRoleIntent(RoleManager.ROLE_DIALER) else null
}
