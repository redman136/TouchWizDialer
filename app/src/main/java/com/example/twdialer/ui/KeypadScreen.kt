package com.example.twdialer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.twdialer.ContactMatch

object TwColors {
    val HeaderTeal = Color(0xFF004C52)
    val TabCyan = Color(0xFF00D4E6)
    val TabActiveText = Color.White
    val TabInactiveText = Color(0xFF9CC5C8)
    val PadBg = Color(0xFFF4F4F4)
    val KeyFill = Color.White.copy(alpha = 0.55f)
    val KeyBorder = Color(0xFFD2D2D2)
    val Slate = Color(0xFF4A5A66)
    val SlateLight = Color(0xFF8795A1)
    val Glow = Color(0xFF00AEBD)
    val GlowCore = Color(0xFF3FE0EE)
    val CallGreen = Color(0xFF3DB335)
    val CallGreenPressed = Color(0xFF2B8C26)
}

enum class DialerTab(val label: String) {
    Keypad("Keypad"), Logs("Logs"), Favorites("Favorites"), Contacts("Contacts")
}

private data class Key(val digit: String, val letters: String = "")

private val KeyRows = listOf(
    listOf(Key("1"), Key("2", "ABC"), Key("3", "DEF")),
    listOf(Key("4", "GHI"), Key("5", "JKL"), Key("6", "MNO")),
    listOf(Key("7", "PQRS"), Key("8", "TUV"), Key("9", "WXYZ")),
    listOf(Key("*"), Key("0", "+"), Key("#")),
)

@Composable
fun KeypadScreen(
    number: String,
    matches: List<ContactMatch>,
    selectedTab: DialerTab,
    onTabSelected: (DialerTab) -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCall: (String) -> Unit,
    onMatchClick: (ContactMatch) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(TwColors.PadBg)) {
        TwTabBar(selectedTab, onTabSelected)

        NumberDisplay(number, onBackspace, onClear)
        Box(Modifier.fillMaxWidth().height(1.dp).background(TwColors.KeyBorder))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(matches, key = { it.name + it.number }) { m ->
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable { onMatchClick(m) }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(m.name, color = TwColors.Slate, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(m.number, color = TwColors.SlateLight, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }

        Column(
            Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KeyRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { key ->
                        TwKey(
                            key = key,
                            modifier = Modifier.weight(1f),
                            onClick = { onDigit(key.digit) },
                            onLongClick = if (key.digit == "0") ({ onDigit("+") }) else null,
                        )
                    }
                }
            }
        }

        CallButton(
            onClick = { onCall(number) },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp, bottom = 14.dp),
        )

        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Composable
private fun TwTabBar(selected: DialerTab, onSelected: (DialerTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(TwColors.HeaderTeal)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        DialerTab.entries.forEach { tab ->
            val isSel = tab == selected
            Box(
                Modifier.weight(1f).height(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelected(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    color = if (isSel) TwColors.TabActiveText else TwColors.TabInactiveText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                )
                if (isSel) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp)
                            .background(TwColors.TabCyan)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberDisplay(number: String, onBackspace: () -> Unit, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp))
        Text(
            text = number,
            modifier = Modifier.weight(1f),
            color = TwColors.Slate,
            fontSize = when {
                number.length > 18 -> 22.sp
                number.length > 12 -> 28.sp
                else -> 36.sp
            },
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        Box(
            Modifier.size(56.dp)
                .combinedClickable(
                    enabled = number.isNotEmpty(),
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBackspace,
                    onLongClick = onClear,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (number.isNotEmpty()) Text("⌫", color = TwColors.SlateLight, fontSize = 26.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TwKey(
    key: Key,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(50)

    val glow by animateFloatAsState(if (pressed) 1f else 0f, tween(if (pressed) 0 else 260), label = "glow")
    val border by animateColorAsState(
        if (pressed) TwColors.Glow else TwColors.KeyBorder, tween(if (pressed) 0 else 260), label = "border"
    )
    val fg by animateColorAsState(
        if (pressed) Color.White else TwColors.Slate, tween(if (pressed) 0 else 260), label = "fg"
    )
    val fgLight by animateColorAsState(
        if (pressed) Color.White.copy(alpha = 0.85f) else TwColors.SlateLight, tween(if (pressed) 0 else 260), label = "fgl"
    )

    Box(
        modifier
            .height(62.dp)
            .clip(shape)
            .background(TwColors.KeyFill)
            .drawBehind {
                if (glow > 0f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(TwColors.GlowCore, TwColors.Glow),
                            center = center,
                            radius = size.maxDimension * 0.6f,
                        ),
                        alpha = glow,
                    )
                }
            }
            .border(0.8.dp, border, shape)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick?.let { lc ->
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        lc()
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(key.digit, color = fg, fontSize = 30.sp, fontWeight = FontWeight.Light)
            if (key.letters.isNotEmpty()) {
                Text(
                    key.letters,
                    color = fgLight,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CallButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(
        if (pressed) TwColors.CallGreenPressed else TwColors.CallGreen, tween(if (pressed) 0 else 200), label = "call"
    )
    Box(
        modifier
            .fillMaxWidth(0.44f)
            .height(54.dp)
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Call, contentDescription = "Call", tint = Color.White, modifier = Modifier.size(28.dp))
    }
}
