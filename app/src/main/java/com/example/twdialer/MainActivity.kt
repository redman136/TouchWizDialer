package com.example.twdialer

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.example.twdialer.ui.DialerTab
import com.example.twdialer.ui.KeypadScreen
import com.example.twdialer.ui.TwColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(TwColors.HeaderTeal.toArgb()),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { DialerRoot() }
    }
}

@Composable
private fun DialerRoot() {
    DialerPermissionGate { perms ->
        val context = LocalContext.current
        var number by rememberSaveable { mutableStateOf("") }
        var tab by rememberSaveable { mutableStateOf(DialerTab.Keypad) }
        var contacts by remember { mutableStateOf(emptyList<IndexedContact>()) }

        LaunchedEffect(perms.contacts) {
            contacts = if (perms.contacts) loadContacts(context) else emptyList()
        }
        val matches = remember(number, contacts) { T9.filter(contacts, number) }

        KeypadScreen(
            number = number,
            matches = matches,
            selectedTab = tab,
            onTabSelected = { tab = it },
            onDigit = { number += it },
            onBackspace = { number = number.dropLast(1) },
            onClear = { number = "" },
            onCall = { placeCall(context, it) },
            onMatchClick = { number = it.number },
        )
    }
}
