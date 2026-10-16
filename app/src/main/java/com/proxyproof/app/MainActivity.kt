
package com.proxyproof.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.proxyproof.app.ui.theme.ProxyProofTheme
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProxyProofTheme {
                var selectedRole by remember { mutableStateOf<String?>(null) }

                if (selectedRole == null) {
                    ProxyProofHome(
                        onTeacherClick = { selectedRole = "teacher" },
                        onStudentClick = { selectedRole = "student" }
                    )
                } else {
                    AuthScreen(
                        role = selectedRole!!,
                        onBack = { selectedRole = null }
                    )
                }
            }
        }
    }
}

@Composable
fun ProxyProofHome(
    onTeacherClick: () -> Unit,
    onStudentClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PROXYPROOF",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Smart Bluetooth Attendance",
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            RoleCard(
                title = "Teacher",
                description = "Start sessions and manage attendance",
                buttonText = "Continue as Teacher",
                onClick = onTeacherClick
            )

            Spacer(Modifier.height(16.dp))

            RoleCard(
                title = "Student",
                description = "Discover classes and view attendance",
                buttonText = "Continue as Student",
                onClick = onStudentClick
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Secure attendance. Less paperwork.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = description,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun AuthScreen(
    role: String,
    onBack: () -> Unit
) {
    var fullName by remember { mutableStateOf("")}
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (role == "teacher") "Teacher Login" else "Student Login",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isRegistering) "Create your account" else "Welcome back"
        )

        Spacer(Modifier.height(24.dp))

        if (isRegistering) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
        }


        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (6+ characters)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (
                    email.isBlank() ||
                    password.length < 6 ||
                    (isRegistering && fullName.isBlank())
                )
                {
                    Toast.makeText(
                        context,
                        "Enter a valid email and a password of at least 6 characters.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    isLoading = true

                    val task = if (isRegistering) {
                        auth.createUserWithEmailAndPassword(
                            email.trim(),
                            password
                        )
                    } else {
                        auth.signInWithEmailAndPassword(
                            email.trim(),
                            password
                        )
                    }


                    task.addOnCompleteListener { result ->
                        if (!result.isSuccessful) {
                            isLoading = false

                            Toast.makeText(
                                context,
                                result.exception?.localizedMessage
                                    ?: "Authentication failed.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnCompleteListener
                        }

                        // Login succeeds without creating a new profile.
                        if (!isRegistering) {
                            isLoading = false

                            Toast.makeText(
                                context,
                                "Login successful!",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnCompleteListener
                        }

                        val user = auth.currentUser

                        if (user == null) {
                            isLoading = false
                            Toast.makeText(
                                context,
                                "Account created, but user details could not be loaded.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addOnCompleteListener
                        }

                        // Self-registration is student-only.
                        if (role != "student") {
                            auth.signOut()
                            isLoading = false

                            Toast.makeText(
                                context,
                                "Teacher accounts must be approved separately.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnCompleteListener
                        }

                        val profile = hashMapOf(
                            "name" to fullName.trim(),
                            "email" to user.email,
                            "role" to "student",
                            "active" to true,
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .set(profile)
                            .addOnSuccessListener {
                                isLoading = false

                                Toast.makeText(
                                    context,
                                    "Account and student profile created!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .addOnFailureListener { error ->
                                isLoading = false

                                Toast.makeText(
                                    context,
                                    "Account created, but profile save failed: " +
                                            (error.localizedMessage ?: "Unknown error"),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }

                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(if (isRegistering) "Create Account" else "Sign In")
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { isRegistering = !isRegistering },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isRegistering) {
                    "Already have an account? Sign in"
                } else {
                    "New here? Create an account"
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

