package com.example.fleerun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.fleerun.ui.theme.FleeRunTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import androidx.compose.material3.ExperimentalMaterial3Api



class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            FleeRunTheme {
                LoginScreen(auth = auth)
            }
        }
    }

    @Composable

    fun LoginScreen(auth: FirebaseAuth) {
        val context = LocalContext.current

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var registerMode by remember { mutableStateOf(false) }
        var selectedRole by remember { mutableStateOf("atleta") }
        var name by remember { mutableStateOf("") }
        var entrenadorEmail by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf("") }

        val firestore = FirebaseFirestore.getInstance()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (registerMode) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (registerMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedRole == "atleta",
                        onClick = { selectedRole = "atleta" }
                    )
                    Text("Atleta")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = selectedRole == "entrenador",
                        onClick = { selectedRole = "entrenador" }
                    )
                    Text("Entrenador")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedRole == "atleta") {
                    TextField(
                        value = entrenadorEmail,
                        onValueChange = { entrenadorEmail = it },
                        label = { Text("Correo del entrenador") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Button(
                onClick = {
                    if (!registerMode) {
                        // INICIAR SESIÓN
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    context.startActivity(Intent(context, WelcomeActivity::class.java))
                                } else {
                                    errorMessage = "Error al iniciar sesión: ${task.exception?.message}"
                                }
                            }
                    } else {
                        // REGISTRARSE
                        if (email.isBlank() || password.isBlank() || name.isBlank() ||
                            (selectedRole == "atleta" && entrenadorEmail.isBlank())
                        ) {
                            errorMessage = "Por favor completa todos los campos"
                            return@Button
                        }

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    val userId = user?.uid ?: return@addOnCompleteListener

                                    val userData = hashMapOf(
                                        "email" to email,
                                        "rol" to selectedRole,
                                        "nombre" to name,
                                        "emailentrenador" to if (selectedRole == "entrenador") email else entrenadorEmail
                                    )


                                    firestore.collection("usuarios").document(userId)
                                        .set(userData)
                                        .addOnSuccessListener {

                                            val profileUpdates = userProfileChangeRequest {
                                                displayName = name
                                            }
                                            user.updateProfile(profileUpdates)

                                            if (selectedRole == "atleta") {

                                                firestore.collection("usuarios")
                                                    .whereEqualTo("email", entrenadorEmail)
                                                    .get()
                                                    .addOnSuccessListener { result ->
                                                        if (!result.isEmpty) {
                                                            val entrenadorDoc = result.documents[0]
                                                            val entrenadorId = entrenadorDoc.id


                                                            firestore.collection("usuarios").document(entrenadorId)
                                                                .update("atletas", FieldValue.arrayUnion(userId))
                                                        }
                                                        context.startActivity(Intent(context, WelcomeActivity::class.java))
                                                    }
                                                    .addOnFailureListener {
                                                        errorMessage = "Entrenador no encontrado"
                                                    }
                                            } else {

                                                context.startActivity(Intent(context, WelcomeActivity::class.java))
                                            }
                                        }
                                        .addOnFailureListener {
                                            errorMessage = "Error al guardar usuario: ${it.message}"
                                        }
                                } else {
                                    errorMessage = "Error al registrar: ${task.exception?.message}"
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (registerMode) "Registrar Cuenta" else "Iniciar Sesión")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    registerMode = !registerMode
                    email = ""
                    password = ""
                    name = ""
                    entrenadorEmail = ""
                    errorMessage = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (registerMode) "Ya tengo cuenta. Iniciar sesión" else "No tengo cuenta. Registrar")
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
