package com.rksaykot.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rksaykot.myapplication.model.Message
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(message: Message) {
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = horizontalAlignment
    ) {
        Surface(
            color = backgroundColor,
            shape = shape,
            tonalElevation = 2.dp
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
                    // Simple Reply UI
                    Surface(
                        color = Color.Black.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            "Replying to a message...",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(4.dp),
                            color = contentColor.copy(alpha = 0.6f)
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
                    Text(
                        text = timeString,
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    if (message.isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isSeen) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (message.isSeen) "Seen" else "Sent",
                            modifier = Modifier.size(14.dp),
                            tint = if (message.isSeen) Color.Cyan else contentColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
