package com.example.twdialer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class TwInCallService : InCallService() {

    private val notificationCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = postNotification(call)
    }

    @Suppress("DEPRECATION")
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.service = this
        CallManager.onCallAdded(applicationContext, call)
        callAudioState?.let { CallManager.onAudioState(it) }
        call.registerCallback(notificationCallback)
        postNotification(call)
        try {
            startActivity(callIntent())
        } catch (_: Exception) {
            // Android may block this; the full-screen notification is the fallback.
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(notificationCallback)
        CallManager.onCallRemoved(call)
        if (calls.isEmpty()) {
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        }
    }

    @Suppress("DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState?.let { CallManager.onAudioState(it) }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        CallManager.service = null
        return super.onUnbind(intent)
    }

    private fun callIntent() =
        Intent(this, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, "Calls", NotificationManager.IMPORTANCE_HIGH)
            channel.setSound(null, null) // the system plays the ringtone itself
            nm.createNotificationChannel(channel)
        }
    }

    private fun postNotification(call: Call) {
        ensureChannel()
        val ringing = call.state == Call.STATE_RINGING
        val pendingIntent = PendingIntent.getActivity(
            this, 0, callIntent(), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(CallManager.info.value.title)
            .setContentText(if (ringing) "Incoming call" else "Ongoing call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
        if (ringing) builder.setFullScreenIntent(pendingIntent, true)
        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Notification permission not granted.
        }
    }

    companion object {
        private const val CHANNEL_ID = "calls"
        private const val NOTIFICATION_ID = 42
    }
}
