package com.rksaykot.myapplication.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rksaykot.myapplication.model.Message
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    replyMessageText: String? = null,
    onSwipeToReply: (Message) -> Unit = {},
    onLongClick: (Message) -> Unit = {}
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
    val backgroundColor = if (message.isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (message.isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    val shape = if (message.isMe) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    val timeString = message.timestamp?.toDate()?.let {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)
    } ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > 150f) {
                            onSwipeToReply(message)
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (offsetX + dragAmount > 0) {
                            offsetX += dragAmount
                        }
                    }
                )
            }
    ) {
        if (offsetX > 50f) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = "Reply",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalAlignment = horizontalAlignment
        ) {
            Surface(
                color = backgroundColor,
                shape = shape,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onLongClick(message) }
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!message.isMe) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (message.replyToId != null) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(contentColor.copy(alpha = 0.1f))
                                .padding(start = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(24.dp)
                                    .background(contentColor.copy(alpha = 0.5f))
                                    .align(Alignment.CenterVertically)
                            )
                            
                            Text(
                                text = replyMessageText ?: "Original message",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                                color = contentColor.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = message.text,
                        color = contentColor,
                        fontSize = 16.sp
                    )

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.reactions.isNotEmpty()) {
                            Text(
                                text = message.reactions.values.joinToString(""),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Text(
                            text = timeString,
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                        if (message.isMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val icon = if (message.isSeen || message.isDelivered) Icons.Default.DoneAll else Icons.Default.Done
                            val tint = if (message.isSeen) Color.Cyan else contentColor.copy(alpha = 0.5f)
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = tint
                            )
                        }
                    }
                }
            }
        }
    }
}
