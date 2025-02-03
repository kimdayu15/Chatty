package com.gems.chatty.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileScreen()
        }
    }
}


@Composable
fun ProfileScreen() {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var newUsername = remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Profile", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(24.dp))

        Text("New Username:")
        BasicTextField(
            value = newUsername.value,
            onValueChange = { newUsername.value = it },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.Gray.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small)
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                if (newUsername.value.isNotBlank()) {
                    updateUsername(
                        currentUser?.uid ?: "",
                        newUsername.value.toString()
                    ) { success ->
                        if (success) {
                            Toast.makeText(context, "Username updated!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(context, ContactsActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Failed to update username.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Username")
        }
    }
}


fun updateUsername(userId: String, newUsername: String, callback: (Boolean) -> Unit) {
    val database = FirebaseDatabase.getInstance().reference
    val userRef = database.child("users").child(userId)


    userRef.child("displayName").setValue(newUsername)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Optionally, update Firebase Auth displayName as well
                val auth = FirebaseAuth.getInstance()
                auth.currentUser?.let { user ->
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(newUsername)
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                callback(true) // Successfully updated username in both database and Firebase Auth
                            } else {
                                callback(false) // Failed to update Firebase Auth displayName
                            }
                        }
                } ?: callback(false) // No user is logged in
            } else {
                callback(false) // Failed to update username in database
            }
        }
}
