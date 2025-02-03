package com.gems.chatty.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.gems.chatty.R
import com.gems.chatty.ui.ContactData
import com.gems.chatty.ui.MessageData
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val receiverUid = intent.getStringExtra("uid")
            val senderUid = intent.getStringExtra("uidSender")
            ChatScreen(senderUid = senderUid, receiverUid = receiverUid)
        }
    }
}

@Composable
fun ChatScreen(senderUid: String?, receiverUid: String?) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val showEmojiKeyboard = remember { mutableStateOf(false) }
    val contact = remember { mutableStateOf(ContactData()) }
    val messageText = remember { mutableStateOf("") }
    val allMessages = remember { mutableStateListOf<MessageData>() }
    val groupedMessages = remember { mutableStateListOf<Pair<String, List<MessageData>>>() }

    if (receiverUid != null && senderUid != null) {
        fetchUsersFromFirebase(receiverUid) { user -> contact.value = user }
        fetchMessagesFromFirebase(senderUid, receiverUid) { messages ->
            allMessages.clear()
            allMessages.addAll(messages)
            groupMessagesByDate(messages, groupedMessages)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MessageInputBar(
                messageText = messageText,
                onMessageTextChanged = { messageText.value = it },
                onSendMessage = {
                    if (messageText.value.isNotBlank()) {
                        sendMessageToFirebase(senderUid, receiverUid, messageText.value)
                        messageText.value = ""
                    }
                },
                onToggleEmojiKeyboard = {
                    showEmojiKeyboard.value = !showEmojiKeyboard.value
                    if (showEmojiKeyboard.value) {
                        keyboardController?.hide()
                    } else {
                        keyboardController?.show()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ContactHeader(contact = contact.value)

            LazyColumn(modifier = Modifier.weight(1f)) {
                groupedMessages.forEach { (date, messages) ->
                    item {
                        MessageDateHeader(date)
                    }
                    items(messages) { message ->
                        val isSender = message.senderId == senderUid
                        MessageBubble(
                            messageText = message.text,
                            isSender = isSender,
                            timestamp = timeStamp(message.timestamp ?: System.currentTimeMillis())
                        )
                    }
                }
            }

            if (showEmojiKeyboard.value) {
                EmojiKeyboard(onEmojiSelected = { emoji ->
                    messageText.value += emoji
                })
            }
        }
    }
}

@Composable
fun MessageInputBar(
    messageText: MutableState<String>,
    onMessageTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onToggleEmojiKeyboard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleEmojiKeyboard) {
            Icon(
                painter = painterResource(R.drawable.round_emoji_emotions_24),
                contentDescription = null
            )
        }

        TextField(
            value = messageText.value,
            onValueChange = onMessageTextChanged,
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape),
            placeholder = { Text("Type a message...") },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedContainerColor = Color(0xA9D3F4FA),
                unfocusedContainerColor = Color(0xA9D3F4FA)
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Send
            )
        )

        IconButton(onClick = onSendMessage) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                tint = Color.Blue
            )
        }
    }
}

@Composable
fun ContactHeader(contact: ContactData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xC9CCE3E3))
            .padding(5.dp, 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        IconButton(onClick = {
            val intent = Intent(context, ContactsActivity::class.java)
            context.startActivity(intent)
        }) {
            Icon(
                painter = painterResource(R.drawable.round_arrow_back_ios_new_24),
                contentDescription = null,
                tint = Color(0xFF114A52)
            )
        }

        Image(
            painter = rememberAsyncImagePainter(model = contact.photoUrl),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = contact.displayName ?: "No name",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text("Online", color = Color.Blue)
        }
    }
}

@Composable
fun MessageDateHeader(date: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = date,
            modifier = Modifier.padding(8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}

@Composable
fun EmojiKeyboard(onEmojiSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xC9CCE3E3))
            .padding(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val emojis = listOf("😊", "😂", "😍", "😎", "🥳", "😢", "😜", "😇")
            emojis.forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 19.sp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onEmojiSelected(emoji) }
                )
            }
        }
    }
}

@Composable
fun MessageBubble(messageText: String?, isSender: Boolean, timestamp: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSender) Arrangement.End else Arrangement.Start
    ) {
        Column {
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .widthIn(min = 50.dp, max = 250.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSender) Color.Blue else Color.LightGray
                )
            ) {
                Text(
                    text = messageText ?: "No message",
                    modifier = Modifier.padding(12.dp),
                    color = if (isSender) Color.White else Color.Black
                )
            }
            Text(timestamp ?: "-", color = Color.Gray)
        }
    }
}

fun groupMessagesByDate(
    messages: List<MessageData>,
    groupedMessages: MutableList<Pair<String, List<MessageData>>>
) {
    val grouped = messages.groupBy { formatDate(it.timestamp ?: System.currentTimeMillis()) }
    groupedMessages.clear()
    grouped.forEach { (date, messages) ->
        groupedMessages.add(date to messages)
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun timeStamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun fetchUsersFromFirebase(receiverUid: String, callback: (ContactData) -> Unit) {
    val reference = FirebaseDatabase.getInstance().reference.child("users").child(receiverUid)
    reference.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val user = snapshot.getValue(ContactData::class.java)
            user?.let { callback(it) }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Error fetching user data: ${error.message}")
        }
    })
}

fun fetchMessagesFromFirebase(
    senderUid: String,
    receiverUid: String,
    callback: (List<MessageData>) -> Unit
) {
    val database = FirebaseDatabase.getInstance().reference
    val chatId = if (senderUid < receiverUid) "$senderUid $receiverUid" else "$receiverUid $senderUid"

    database.child("chats").child(chatId).child("messages")
        .orderByChild("timestamp")
        .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messageList = mutableListOf<MessageData>()
                for (child in snapshot.children) {
                    val message = child.getValue(MessageData::class.java)
                    message?.let { messageList.add(it) }

                    if (message?.receiverId == senderUid && !message.isSeen) {
                        markMessageAsRead(message.uid.toString())
                    }
                }
                callback(messageList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching messages: ${error.message}")
            }
        })
}

fun markMessageAsRead(messageId: String) {
    val database = FirebaseDatabase.getInstance().reference
    database.child("chats")
        .child(messageId)
        .child("messages")
        .child(messageId)
        .updateChildren(mapOf("isRead" to true))
}

fun sendMessageToFirebase(senderUid: String?, receiverUid: String?, message: String) {
    if (senderUid == null || receiverUid == null || message.isEmpty()) return

    val database = FirebaseDatabase.getInstance().reference
    val chatId = if (senderUid < receiverUid) "$senderUid $receiverUid" else "$receiverUid $senderUid"
    val messageId = database.child("chats").child(chatId).child("messages").push().key ?: return

    val messageData = MessageData(
        senderId = senderUid,
        receiverId = receiverUid,
        text = message,
        timestamp = System.currentTimeMillis(),
        status = "sent",
        isSeen = false
    )

    database.child("chats").child(chatId).child("messages").child(messageId).setValue(messageData)
}
