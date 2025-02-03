package com.gems.chatty.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gems.chatty.R
import com.gems.chatty.ui.data.ContactData
import com.gems.chatty.ui.theme.ChattyTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.database


class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChattyTheme {
                val sharedPreferences: SharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                val savedUid = sharedPreferences.getString("uid", null)

                if (savedUid != null) {
                    val intent = Intent(this, ContactsActivity()::class.java)
                    intent.putExtra("uid", savedUid)
                    startActivity(intent)
                    finish()
                }

                auth = FirebaseAuth.getInstance()
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.your_web_client_id))
                    .requestEmail()
                    .build()

                val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)


                val brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFC1DDEE)))
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(R.drawable.chatty_logo),
                                contentDescription = "Logo",
                                modifier = Modifier.size(130.dp)
                            )

                            Text("Chatty", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Chat with friends and family")
                        }

                        Button(
                            onClick = {
                                val signInIntent = mGoogleSignInClient.signInIntent
                                startActivityForResult(signInIntent, 1)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(49.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.Gray),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.google),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign up with Google",
                                style = TextStyle(
                                    color = Color.Black,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            )
                        }


                        Button(
                            onClick = { mGoogleSignInClient.signOut() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .height(49.dp)
                        ) {
                            Text("Sign out", fontSize = 16.sp)
                        }


                    }
                }
            }
        }
    }




    @Deprecated("result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken)
                Log.d("TAG", "onActivityResult: ")
            } catch (e: ApiException) {
                Log.d("TAG", "error: ${e.message}")

            }
        }
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val sharedPreferences: SharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userData = ContactData(
                        user?.displayName,
                        user?.uid,
                        user?.email,
                        user?.photoUrl.toString()
                    )
                    val userIdReference = Firebase.database.reference
                        .child("users").child(userData.uid ?: "")
                    userIdReference.setValue(userData).addOnCompleteListener({
                        sharedPreferences.edit().putString("uid", userData.uid).apply()
                        val i = Intent(this, ContactsActivity::class.java)
                        i.putExtra("uid", userData.uid)
                        startActivity(i)
                    })

                }
            }
    }
}