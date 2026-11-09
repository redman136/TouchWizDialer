package com.example.twdialer

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.example.twdialer.ui.CallEndedScreen
import com.example.twdialer.ui.IncomingCallScreen
import com.example.twdialer.ui.OngoingCallScreen
import kotlinx.coroutines.delay

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        setContent {
            InCallRoot(onClose = {
                CallManager.reset()
                finish()
            })
        }
    }
}

@Composable
private fun InCallRoot(onClose: () -> Unit) {
    val context = LocalContext.current
    val info by CallManager.info.collectAsState()
    val status = statusText(info)

    BackHandler(enabled = info.phase == CallPhase.Ended) { onClose() }

    when (info.phase) {
        CallPhase.None -> LaunchedEffect(Unit) { onClose() }

        CallPhase.Incoming -> IncomingCallScreen(
            name = info.title,
            onAnswer = CallManager::answer,
            onReject = CallManager::reject,
            onRejectWithMessage = CallManager::reject,
        )

        CallPhase.Dialing, CallPhase.Active, CallPhase.Holding -> OngoingCallScreen(
            name = info.title,
            number = if (info.name != null) info.number else null,
            elapsed = status,
            photo = info.photo,
            speakerOn = info.speaker,
            muted = info.muted,
            bluetoothOn = info.bluetooth,
            onEndCall = CallManager::hangUp,
            onSpeaker = CallManager::toggleSpeaker,
            onMute = CallManager::toggleMute,
            onHeadset = CallManager::toggleBluetooth,
            onHold = CallManager::toggleHold,
        )

        CallPhase.Ended -> CallEndedScreen(
            title = info.title,
            photo = info.photo,
            onCreateContact = { openCreateContact(context, info.number) },
            onUpdateExisting = { openUpdateContact(context, info.number) },
            onCall = {
                val number = info.number
                onClose()
                placeCall(context.applicationContext, number)
            },
            onMessage = { openSms(context, info.number) },
        )
    }
}

@Composable
private fun statusText(info: CallInfo): String {
    val now by produceState(System.currentTimeMillis(), info.connectTimeMillis) {
        while (true) {
            value = System.currentTimeMillis()
            delay(500)
        }
    }
    if (info.phase == CallPhase.Dialing) return "Calling…"
    if (info.phase == CallPhase.Holding) return "On hold"
    val start = if (info.connectTimeMillis > 0) info.connectTimeMillis else now
    val s = ((now - start) / 1000).coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    else "%02d:%02d".format(s / 60, s % 60)
}

private fun openCreateContact(context: Context, number: String) {
    val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        putExtra(ContactsContract.Intents.Insert.PHONE, number)
    }
    runCatching { context.startActivity(intent) }
}

private fun openUpdateContact(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
        type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
        putExtra(ContactsContract.Intents.Insert.PHONE, number)
    }
    runCatching { context.startActivity(intent) }
}

private fun openSms(context: Context, number: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))) }
}
