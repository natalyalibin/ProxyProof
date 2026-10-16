
package com.proxyproof.app

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AuthScreen(
    role: String,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
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
            if (isRegistering) "Create your account" else "Welcome back"
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
                ) {
                    Toast.makeText(
                        context,
                        "Enter valid details. Password must be at least 6 characters.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                // Teacher accounts must be provisioned by an administrator.
                if (isRegistering && role != "student") {
                    Toast.makeText(
                        context,
                        "Teacher accounts require administrator approval.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

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

                    val user = auth.currentUser
                    if (user == null) {
                        isLoading = false
                        Toast.makeText(
                            context,
                            "Could not load account details.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnCompleteListener
                    }

                    if (!isRegistering) {
                        // Do not trust the selected role as authorization.
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                isLoading = false

                                val storedRole =
                                    document.getString("role")
                                val active =
                                    document.getBoolean("active") == true

                                if (
                                    document.exists() &&
                                    active &&
                                    storedRole == role
                                ) {
                                    onAuthenticated()
                                } else {
                                    auth.signOut()
                                    Toast.makeText(
                                        context,
                                        "This account is not authorized for that role.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .addOnFailureListener { error ->
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Could not verify account role: ${
                                        error.localizedMessage ?: "try again"
                                    }",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

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
                            onAuthenticated()
                        }
                        .addOnFailureListener { error ->
                            isLoading = false
                            Toast.makeText(
                                context,
                                "Account created, but profile save failed: ${
                                    error.localizedMessage ?: "try again"
                                }",
                                Toast.LENGTH_LONG
                            ).show()
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
