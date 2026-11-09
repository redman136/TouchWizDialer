package com.example.twdialer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class CallPhase { None, Incoming, Dialing, Active, Holding, Ended }

data class CallInfo(
    val phase: CallPhase = CallPhase.None,
    val number: String = "",
    val name: String? = null,
    val photo: ImageBitmap? = null,
    val connectTimeMillis: Long = 0L,
    val muted: Boolean = false,
    val speaker: Boolean = false,
    val bluetooth: Boolean = false,
) {
    val title: String get() = name ?: number.ifBlank { "Unknown" }
}

private class Found(val name: String?, val photo: ImageBitmap?)

@Suppress("DEPRECATION")
object CallManager {
    private val _info = MutableStateFlow(CallInfo())
    val info: StateFlow<CallInfo> = _info

    private var call: Call? = null
    private var isIncoming = false
    private var everActive = false

    var service: InCallService? = null

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = refresh(call)
        override fun onDetailsChanged(call: Call, details: Call.Details) = refresh(call)
    }

    fun onCallAdded(context: Context, c: Call) {
        call?.unregisterCallback(callback)
        call = c
        isIncoming = c.state == Call.STATE_RINGING
        everActive = false

        val number = c.details.handle?.schemeSpecificPart.orEmpty()
        val found = lookup(context, number)
        val previous = _info.value
        _info.value = CallInfo(
            phase = phaseOf(c.state),
            number = number,
            name = found.name ?: c.details.callerDisplayName?.takeIf { it.isNotBlank() },
            photo = found.photo,
            connectTimeMillis = c.details.connectTimeMillis,
            muted = previous.muted,
            speaker = previous.speaker,
            bluetooth = previous.bluetooth,
        )
        c.registerCallback(callback)
    }

    fun onCallRemoved(c: Call) {
        c.unregisterCallback(callback)
        if (c !== call) return
        call = null
        _info.update {
            if (it.phase == CallPhase.Ended || it.phase == CallPhase.None) it
            else it.copy(phase = endedPhase())
        }
    }

    fun onAudioState(a: CallAudioState) {
        _info.update {
            it.copy(
                muted = a.isMuted,
                speaker = a.route == CallAudioState.ROUTE_SPEAKER,
                bluetooth = a.route == CallAudioState.ROUTE_BLUETOOTH,
            )
        }
    }

    // Controls used by the call screens
    fun answer() { call?.answer(VideoProfile.STATE_AUDIO_ONLY) }
    fun reject() { call?.reject(false, null) }
    fun hangUp() { call?.disconnect() }

    fun toggleHold() {
        val c = call ?: return
        if (c.state == Call.STATE_HOLDING) c.unhold() else c.hold()
    }

    fun toggleMute() { service?.setMuted(!_info.value.muted) }

    fun toggleSpeaker() {
        service?.setAudioRoute(
            if (_info.value.speaker) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER
        )
    }

    fun toggleBluetooth() {
        service?.setAudioRoute(
            if (_info.value.bluetooth) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_BLUETOOTH
        )
    }

    /** Called when the call screen is closed and no call is in progress. */
    fun reset() {
        if (call == null) _info.value = CallInfo()
    }

    private fun refresh(c: Call) {
        if (c !== call) return
        var phase = phaseOf(c.state)
        if (phase == CallPhase.Active) everActive = true
        if (phase == CallPhase.Ended) phase = endedPhase()
        _info.update { it.copy(phase = phase, connectTimeMillis = c.details.connectTimeMillis) }
    }

    // An incoming call that was never answered (rejected or missed) skips the "Call ended" screen.
    private fun endedPhase() = if (isIncoming && !everActive) CallPhase.None else CallPhase.Ended

    private fun phaseOf(state: Int) = when (state) {
        Call.STATE_RINGING -> CallPhase.Incoming
        Call.STATE_ACTIVE -> CallPhase.Active
        Call.STATE_HOLDING -> CallPhase.Holding
        Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> CallPhase.Ended
        else -> CallPhase.Dialing
    }

    private fun lookup(context: Context, number: String): Found {
        if (number.isBlank()) return Found(null, null)
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return Found(null, null)
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI)
            context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(0)
                    val photoUri = c.getString(1)
                    val bitmap = photoUri?.let {
                        context.contentResolver.openInputStream(Uri.parse(it))?.use { s ->
                            BitmapFactory.decodeStream(s)?.asImageBitmap()
                        }
                    }
                    Found(name, bitmap)
                } else Found(null, null)
            } ?: Found(null, null)
        } catch (e: Exception) {
            Found(null, null)
        }
    }
}
