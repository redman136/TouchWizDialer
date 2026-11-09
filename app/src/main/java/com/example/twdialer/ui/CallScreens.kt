package com.example.twdialer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val CallRed = Color(0xFFE5311B)
private val CallGreenHeader = Color(0xFF74A94B)
private val CallOrangeHeader = Color(0xFFFF8F5E)
private val PhotoBlue = Color(0xFFA5BAD9)
private val KeyIconColor = Color(0xFFE8EEF2)

/* ---------------------------------------------------------------- INCOMING */

@Composable
fun IncomingCallScreen(
    name: String,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onRejectWithMessage: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        CallHeader(Color(0xFF121212), name, null, "Incoming call")
        Box(
            Modifier.weight(1f).fillMaxWidth().background(Color(0xFF1B1B1B)),
            contentAlignment = Alignment.Center,
        ) { RingingGlyph() }
        SwipeRow(onAnswer, onReject)
        RejectWithMessageBar(onRejectWithMessage)
    }
}

@Composable
private fun RingingGlyph() {
    val transition = rememberInfiniteTransition(label = "ring")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    Box(Modifier.size(width = 270.dp, height = 230.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val origin = Offset(105.dp.toPx(), 120.dp.toPx())
            val blue = Color(0xFF1AA7EC)
            // 4 waves, one after another, smallest to biggest, then the loop restarts
            for (i in 0 until 4) {
                val appear = ((t - i) / 0.4f).coerceIn(0f, 1f)
                val fade = ((5f - t) / 0.5f).coerceIn(0f, 1f)
                val r = (42 + i * 24).dp.toPx()
                drawArc(
                    color = blue,
                    startAngle = -80f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(origin.x - r, origin.y - r),
                    size = Size(r * 2, r * 2),
                    alpha = minOf(appear, fade),
                    style = Stroke(width = (3 + i).dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        Icon(
            Icons.Filled.Call,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 45.dp).size(120.dp),
        )
    }
}

@Composable
private fun SwipeRow(onAnswer: () -> Unit, onReject: () -> Unit) {
    val scope = rememberCoroutineScope()
    val greenX = remember { Animatable(0f) }
    val redX = remember { Animatable(0f) }
    val circle = 74.dp
    val edge = 20.dp

    BoxWithConstraints(Modifier.fillMaxWidth().height(150.dp).background(Color.Black)) {
        val travel = with(LocalDensity.current) { (maxWidth - circle - edge * 2).toPx() }

        SwipeArrows(Modifier.align(Alignment.Center))

        // Swipe RIGHT on green to answer
        Box(
            Modifier.align(Alignment.CenterStart).padding(start = edge)
                .offset { IntOffset(greenX.value.roundToInt(), 0) }
                .size(circle)
                .border(2.dp, TwColors.CallGreen, CircleShape)
                .pointerInput(travel) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (greenX.value > travel * 0.5f) onAnswer()
                            else scope.launch { greenX.animateTo(0f) }
                        },
                        onDragCancel = { scope.launch { greenX.animateTo(0f) } },
                    ) { change, drag ->
                        change.consume()
                        scope.launch { greenX.snapTo((greenX.value + drag).coerceIn(0f, travel)) }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Call, contentDescription = "Answer", tint = TwColors.CallGreen, modifier = Modifier.size(38.dp))
        }

        // Swipe LEFT on red to reject
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = edge)
                .offset { IntOffset(-redX.value.roundToInt(), 0) }
                .size(circle)
                .border(2.dp, CallRed, CircleShape)
                .pointerInput(travel) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (redX.value > travel * 0.5f) onReject()
                            else scope.launch { redX.animateTo(0f) }
                        },
                        onDragCancel = { scope.launch { redX.animateTo(0f) } },
                    ) { change, drag ->
                        change.consume()
                        scope.launch { redX.snapTo((redX.value - drag).coerceIn(0f, travel)) }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CallEnd, contentDescription = "Reject", tint = CallRed, modifier = Modifier.size(38.dp))
        }
    }
}

private fun chase(t: Float, i: Int): Float = (1f - ((t + 4f - i) % 4f) / 4f).coerceIn(0.2f, 1f)

@Composable
private fun SwipeArrows(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "arrows")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "t",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            repeat(4) { i ->
                Icon(
                    Icons.Filled.PlayArrow, contentDescription = null,
                    tint = TwColors.CallGreen.copy(alpha = chase(t, i)),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Row {
            repeat(4) { i ->
                Icon(
                    Icons.Filled.PlayArrow, contentDescription = null,
                    tint = CallRed.copy(alpha = chase(t, 3 - i)),
                    modifier = Modifier.size(22.dp).rotate(180f),
                )
            }
        }
    }
}

@Composable
private fun RejectWithMessageBar(onClick: () -> Unit) {
    val gray = Color(0xFF8A8A8A)
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.align(Alignment.CenterHorizontally).width(110.dp).height(22.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(gray),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.fillMaxWidth().background(gray).clickable(onClick = onClick)) {
            Row(
                Modifier.fillMaxWidth().height(52.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Email, contentDescription = null, tint = Color(0xFFF5C518), modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text("Reject call with message", color = Color.White, fontSize = 16.sp)
            }
            Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

/* ----------------------------------------------------------------- ONGOING */

@Composable
fun OngoingCallScreen(
    name: String,
    number: String?,
    elapsed: String,
    photo: ImageBitmap? = null,
    speakerOn: Boolean = false,
    muted: Boolean = false,
    bluetoothOn: Boolean = false,
    onRecord: () -> Unit = {},
    onKeypad: () -> Unit = {},
    onEndCall: () -> Unit,
    onSpeaker: () -> Unit = {},
    onMute: () -> Unit = {},
    onHeadset: () -> Unit = {},
    onHold: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        CallHeader(CallGreenHeader, name, number, elapsed)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            ContactPhoto(photo, Modifier.fillMaxSize())
            Box(
                Modifier.align(Alignment.TopEnd).padding(10.dp).size(46.dp)
                    .clip(CircleShape).background(Color(0xFF30363E)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Equalizer, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        HoldBar(onHold)

        Column(
            Modifier.padding(horizontal = 3.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                CallKey("Record", Modifier.weight(1f), onRecord) { KeyIcon(Icons.Filled.Voicemail) }
                CallKey("Keypad", Modifier.weight(1f), onKeypad) { KeyIcon(Icons.Filled.Dialpad) }
                CallKey("End call", Modifier.weight(1f), onEndCall) { KeyIcon(Icons.Filled.CallEnd, CallRed) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                CallKey("Speaker", Modifier.weight(1f), onSpeaker, indicator = speakerOn) { KeyIcon(Icons.Filled.VolumeUp) }
                CallKey("Mute", Modifier.weight(1f), onMute, indicator = muted) { KeyIcon(Icons.Filled.MicOff) }
                CallKey("Headset", Modifier.weight(1f), onHeadset, indicator = bluetoothOn) { KeyIcon(Icons.Filled.Bluetooth) }
            }
        }
        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Composable
private fun HoldBar(onHold: () -> Unit) {
    val divider = Color.White.copy(alpha = 0.25f)
    Row(
        Modifier.fillMaxWidth().height(48.dp).background(Color(0xFF6F7E96)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Color(0xFF8BE000))
        }
        Box(Modifier.width(1.dp).height(28.dp).background(divider))
        Row(
            Modifier.weight(1f).fillMaxHeight().clickable(onClick = onHold),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Pause, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(6.dp))
            Text("Hold", color = Color.White, fontSize = 18.sp)
        }
        Box(Modifier.width(1.dp).height(28.dp).background(divider))
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PhoneInTalk, contentDescription = null, tint = Color.White)
        }
    }
}

/* ------------------------------------------------------------------- ENDED */

@Composable
fun CallEndedScreen(
    title: String,
    photo: ImageBitmap? = null,
    onCreateContact: () -> Unit = {},
    onUpdateExisting: () -> Unit = {},
    onVideoCall: () -> Unit = {},
    onCall: () -> Unit = {},
    onMessage: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        CallHeader(CallOrangeHeader, title, null, "Call ended")
        ContactPhoto(photo, Modifier.fillMaxWidth().weight(2f))
        Row(
            Modifier.fillMaxWidth().padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            PillButton("Create contact", Modifier.weight(1f), onCreateContact)
            PillButton("Update existing", Modifier.weight(1f), onUpdateExisting)
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            CallKey("Video call", Modifier.weight(1f), onVideoCall) { KeyIcon(Icons.Filled.Videocam, TwColors.CallGreen) }
            CallKey("Call", Modifier.weight(1f), onCall) { KeyIcon(Icons.Filled.Call, TwColors.CallGreen) }
            CallKey("Message", Modifier.weight(1f), onMessage) { KeyIcon(Icons.Filled.Email, Color(0xFFF5C518)) }
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Composable
private fun PillButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF59656F)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 14.sp) }
}

/* ------------------------------------------------------------------ SHARED */

@Composable
private fun CallHeader(color: Color, title: String, subtitle: String?, status: String?) {
    Box(Modifier.fillMaxWidth().background(color).windowInsetsPadding(WindowInsets.statusBars)) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 96.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(title, color = Color.White, fontSize = 30.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp, maxLines = 1)
                }
            }
            if (status != null) {
                Text(status, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Contact photo if there is one, otherwise the white silhouette on light blue. */
@Composable
fun ContactPhoto(photo: ImageBitmap?, modifier: Modifier = Modifier) {
    Box(modifier.background(PhotoBlue)) {
        if (photo != null) {
            Image(bitmap = photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val white = Color(0xFFF2F6FB)
                val headW = minOf(w, h) * 0.42f
                val headH = headW * 1.12f
                val cx = w / 2
                val headTop = h * 0.1f
                drawOval(color = white, topLeft = Offset(cx - w * 0.30f, h * 0.66f), size = Size(w * 0.60f, h * 0.9f))
                drawOval(color = white, topLeft = Offset(cx - headW / 2, headTop), size = Size(headW, headH))
                drawCircle(PhotoBlue, headW * 0.04f, Offset(cx - headW * 0.22f, headTop + headH * 0.42f))
                drawCircle(PhotoBlue, headW * 0.04f, Offset(cx + headW * 0.22f, headTop + headH * 0.42f))
                drawArc(
                    color = PhotoBlue,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx - headW * 0.15f, headTop + headH * 0.55f),
                    size = Size(headW * 0.3f, headW * 0.2f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun KeyIcon(vector: ImageVector, tint: Color = KeyIconColor) {
    Icon(vector, contentDescription = null, tint = tint, modifier = Modifier.size(38.dp))
}

@Composable
private fun CallKey(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
    indicator: Boolean? = null,
    icon: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(2.dp)
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val colors = if (pressed) listOf(TwColors.Glow, TwColors.Glow) else listOf(Color(0xFF2A4663), Color(0xFF1B3149))
    Box(
        modifier.height(92.dp).clip(shape)
            .background(Brush.verticalGradient(colors))
            .border(1.dp, Color(0xFF3B5876), shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(Modifier.height(4.dp))
            Text(label, color = Color.White, fontSize = 17.sp, maxLines = 1)
        }
        if (indicator != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                    .width(70.dp).height(4.dp)
                    .background(if (indicator) Color(0xFF8BE000) else Color(0xFF6F7F8F))
            )
        }
    }
}
