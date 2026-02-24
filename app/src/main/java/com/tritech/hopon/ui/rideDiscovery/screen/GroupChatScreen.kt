package com.tritech.hopon.ui.rideDiscovery.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tritech.hopon.R
import com.tritech.hopon.ui.rideDiscovery.core.MockChatMessage

@Composable
fun groupChatScreen(
    currentUserId: String?,
    participants: List<String>,
    messages: List<MockChatMessage>,
    onSendMessage: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var draftMessage by remember { mutableStateOf("") }
    val primaryTint = colorResource(id = R.color.colorPrimary)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(id = R.string.group_chat_back)
                )
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(id = R.string.group_chat),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = primaryTint
            )
        }

        if (participants.isNotEmpty()) {
            Text(
                text = participants.joinToString(separator = " • "),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
            )
        }

        HorizontalDivider()

        if (messages.isEmpty()) {
            Text(
                text = stringResource(id = R.string.group_chat_no_messages),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isMine = currentUserId != null && currentUserId == message.senderUserId
                    val bubbleColor = if (isMine) {
                        primaryTint
                    } else {
                        primaryTint.copy(alpha = 0.12f)
                    }
                    val messageColor = if (isMine) Color.White else Color.Black

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = bubbleColor),
                            border = BorderStroke(1.dp, primaryTint.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(260.dp)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = message.senderDisplayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isMine) Color.White.copy(alpha = 0.85f) else Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = message.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = messageColor,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = message.sentAtLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMine) Color.White.copy(alpha = 0.85f) else Color.Gray,
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        val imeVisible = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current) > 0

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (imeVisible) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draftMessage,
                onValueChange = { draftMessage = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(text = stringResource(id = R.string.group_chat_input_placeholder))
                },
                singleLine = true
            )

            Button(
                onClick = {
                    val text = draftMessage.trim()
                    if (text.isNotEmpty()) {
                        onSendMessage(text)
                        draftMessage = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryTint,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(id = R.string.group_chat_send)
                )
            }
        }
    }
}