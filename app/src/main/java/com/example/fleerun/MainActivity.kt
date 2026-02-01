package com.example.fleerun

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.fleerun.ui.theme.FleeRunTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuthUserCollisionException



class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser

        if (user != null) {

            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            finish()
        } else {

            setContent {
                FleeRunTheme {
                    LoginScreen(auth = auth)
                }
            }
        }
    }

    @Composable
    fun LoginScreen(auth: FirebaseAuth) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var registerMode by remember { mutableStateOf(false) }
        var selectedRole by remember { mutableStateOf("atleta") }
        var errorMessage by remember { mutableStateOf("") }
        var entrenadorEmail by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(value = email, onValueChange = { email = it }, label = { Text("Correo Electrónico") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))

            TextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))

            if (registerMode) {
                TextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirmar Contraseña") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))

                TextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    RadioButton(selected = selectedRole == "atleta", onClick = { selectedRole = "atleta" })
                    Text("Atleta")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = selectedRole == "entrenador", onClick = { selectedRole = "entrenador" })
                    Text("Entrenador")
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedRole == "atleta") {
                    TextField(value = entrenadorEmail, onValueChange = { entrenadorEmail = it }, label = { Text("Correo de tu entrenador") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Botones para login y registro
            if (!registerMode) {
                Button(onClick = {
                    if (email.isEmpty() || password.isEmpty()) {
                        errorMessage = "Por favor, ingresa tu correo y contraseña."
                        return@Button
                    }

                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val intent = Intent(this@MainActivity, WelcomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                errorMessage = "Error al iniciar sesión: ${task.exception?.message}"
                                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Iniciar Sesión")
                }
            } else {
                Button(onClick = {
                    if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                        errorMessage = "Por favor, ingresa todos los campos."
                        return@Button
                    }

                    if (password != confirmPassword) {
                        errorMessage = "Las contraseñas no coinciden."
                        return@Button
                    }

                    if (password.length < 6) {
                        errorMessage = "La contraseña debe tener al menos 6 caracteres."
                        return@Button
                    }

                    if (selectedRole == "atleta" && entrenadorEmail.isEmpty()) {
                        errorMessage = "Por favor, ingresa el correo de tu entrenador."
                        return@Button
                    }

                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val firestore = FirebaseFirestore.getInstance()

                                val userData = hashMapOf(
                                    "email" to email,
                                    "rol" to selectedRole,
                                    "name" to name
                                )

                                if (selectedRole == "atleta") {
                                    userData["entrenadorEmail"] = entrenadorEmail
                                }

                                user?.let {
                                    firestore.collection("usuarios").document(it.uid)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            val intent = Intent(this@MainActivity, WelcomeActivity::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { exception ->
                                            errorMessage = "Error al guardar datos en Firestore: ${exception.message}"
                                        }
                                }
                            } else {
                                if (task.exception is FirebaseAuthUserCollisionException) {
                                    errorMessage = "Este correo electrónico ya está registrado. Intenta con otro."
                                } else {
                                    errorMessage = "Error al registrar usuario: ${task.exception?.message}"
                                }
                                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Registrar Cuenta")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                registerMode = !registerMode
                email = ""
                password = ""
                confirmPassword = ""
                name = ""
                entrenadorEmail = ""
                errorMessage = ""
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (registerMode) "Ya tengo cuenta. Iniciar sesión" else "No tengo cuenta. Registrar")
            }

            if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

