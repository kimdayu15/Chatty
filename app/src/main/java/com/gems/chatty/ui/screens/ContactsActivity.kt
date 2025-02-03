package com.gems.chatty.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gems.chatty.ui.data.ContactData
import com.gems.chatty.ui.theme.ChattyTheme
import com.gems.chatty.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.gems.chatty.ui.data.MessageData
import kotlinx.coroutines.launch


class ContactsActivity() : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val contactList = remember { mutableStateListOf<ContactData>() }
            var isSearching = remember { mutableStateOf(false) }
            var searchQuery = remember { mutableStateOf("") }

            val unreadCount = remember { mutableStateOf(0) }

            ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 17.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.chatty_logo),
                                        contentDescription = null, modifier = Modifier.size(37.dp)
                                    )
                                    Text(
                                        "Chatty",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                                IconButton(onClick = { scope.launch { drawerState.close() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null
                                    )
                                }
                            }
                            HorizontalDivider()

                            Text(
                                "Messages",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.padding(7.dp))
                                        Text("My Profile")
                                    }
                                },
                                selected = false,
                                onClick = {
                                    val intent = Intent(this@ContactsActivity, ProfileActivity::class.java)
                                    startActivity(intent)
                                }
                            )
                            NavigationDrawerItem(
                                label = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_people_24),
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.padding(7.dp))
                                        Text("Contacts (2 online)")
                                    }
                                },
                                selected = false,
                                onClick = { }
                            )
                        }
                    }
                },
                drawerState = drawerState,
                gesturesEnabled = true,
                scrimColor = Color.Black.copy(alpha = 0.3f)
            ) {
                ChattyTheme {
                    val uid = intent.getStringExtra("uid")
                    if (uid != null) {
                        fetchUsersFromFirebase { userList ->
                            contactList.clear()
                            contactList.addAll(userList)
                        }
                    }

                    Scaffold(
                        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 20.dp),
                        topBar = {
                            TopAppBar(
                                title = {
                                    if (isSearching.value) {
                                        TextField(
                                            value = searchQuery.value,
                                            onValueChange = { searchQuery.value = it },
                                            placeholder = { Text("Search") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(CircleShape),
                                            colors = TextFieldDefaults.colors(
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                disabledIndicatorColor = Color.Transparent,
                                                focusedContainerColor = Color(0x85FFFFFF),
                                                unfocusedContainerColor = Color(0x85FFFFFF)
                                            ),
                                            textStyle = TextStyle.Default.copy(fontSize = 17.sp)
                                        )
                                    } else {
                                        Text("Contacts", fontWeight = FontWeight.Bold)
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch {
                                            if (drawerState.isClosed) drawerState.open()
                                            else drawerState.close()
                                        }
                                    }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                },
                                actions = {
                                    IconButton(onClick = {
                                        if (isSearching.value) {
                                            searchQuery.value = ""
                                            isSearching.value = false
                                        } else {
                                            isSearching.value = true
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (isSearching.value) Icons.Default.Close else Icons.Default.Search,
                                            contentDescription = if (isSearching.value) "Close" else "Search"
                                        )
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {
                            val brush =
                                Brush.verticalGradient(listOf(Color.White, Color(0xFFCCE7F6)))
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(brush)
                            ) {
                                val filteredContacts = if (searchQuery.value.isEmpty()) {
                                    contactList
                                } else {
                                    contactList.filter {
                                        it.displayName?.contains(
                                            searchQuery.value,
                                            ignoreCase = true
                                        ) == true ||
                                                it.email?.contains(
                                                    searchQuery.value,
                                                    ignoreCase = true
                                                ) == true
                                    }
                                }

                                if (filteredContacts.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (contactList.isEmpty()) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(48.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Text("No contacts found", color = Color.Gray)
                                        }
                                    }
                                } else {
                                    LazyColumn {
                                        items(filteredContacts) { contact ->
                                            if (contact.uid != uid) {
                                                val lastMessage = remember { mutableStateOf<String?>(null) }

                                                LaunchedEffect(contact.uid) {
                                                    fetchLastMessage(uid.toString(),
                                                        contact.uid.toString()
                                                    ) { message ->
                                                        lastMessage.value = message
                                                    }
                                                }

                                                val displayMessage = lastMessage.value?.let {
                                                    if (it.length > 30) {
                                                        "${it.take(30)}..."
                                                    } else {
                                                        it
                                                    }
                                                } ?: "No messages yet"

                                                EachContact(contact = contact, lastMessage = displayMessage) {
                                                    val intent = Intent(this@ContactsActivity, ChatActivity::class.java)
                                                    intent.putExtra("uid", contact.uid)
                                                    intent.putExtra("uidSender", uid)
                                                    startActivity(intent)
                                                }
                                            }
                                        }
                                    }


                                }
                            }
                        }
                    }
                }
            }
        }
    }

}


@Composable
fun EachContact(contact: ContactData, lastMessage: String?, onClick: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = rememberAsyncImagePainter(model = contact.photoUrl),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(text = contact.displayName ?: "Unknown", fontWeight = FontWeight.Bold)
            Text(
                text = lastMessage ?: "Say hi!",
                color = Color.Gray
            )
        }


    }
    HorizontalDivider()
}


fun fetchLastMessage(senderUid: String, receiverUid: String, callback: (String?) -> Unit) {
    val database = FirebaseDatabase.getInstance().reference
    val chatId =
        if (senderUid < receiverUid) "$senderUid $receiverUid" else "$receiverUid $senderUid"

    database.child("chats").child(chatId).child("messages")
        .orderByChild("timestamp")
        .limitToLast(1)
        .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lastMessage = snapshot.children.firstOrNull()?.getValue(MessageData::class.java)
                callback(lastMessage?.text)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching last message: ${error.message}")
                callback(null)
            }
        })
}


private fun fetchUsersFromFirebase(callback: (List<ContactData>) -> Unit) {
    val reference = FirebaseDatabase.getInstance().reference.child("users")
    reference.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val userList = mutableListOf<ContactData>()
            for (child in snapshot.children) {
                val user = child.getValue(ContactData::class.java)
                user?.let { userList.add(it) }
            }
            callback(userList)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("TAG", "Failed to load contacts: ${error.message}")
        }
    })
}
