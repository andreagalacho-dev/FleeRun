package com.example.fleerun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fleerun.ui.theme.FleeRunTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState


class ListaAtletasActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    data class Atleta(val name: String, val email: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setContent {
            FleeRunTheme {
                AtletasScreen()
            }
        }
    }

    @Composable
    fun AtletasScreen() {
        var atletasList by remember { mutableStateOf<List<Atleta>>(emptyList()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        val entrenadorEmail = auth.currentUser?.email

        LaunchedEffect(entrenadorEmail) {
            if (entrenadorEmail != null) {
                firestore.collection("usuarios")
                    .whereEqualTo("entrenadorEmail", entrenadorEmail)
                    .whereEqualTo("rol", "atleta")
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            errorMessage = "No hay atletas asociados a tu cuenta."
                        } else {
                            // Obtener tanto el nombre como el correo de los atletas
                            atletasList = documents.mapNotNull {
                                val name = it.getString("name") // Obtener el nombre
                                val email = it.getString("email") // Obtener el correo
                                if (name != null && email != null) {
                                    Atleta(name, email) // Crear un objeto Atleta con el nombre y correo
                                } else {
                                    null // Si alguno es null, no se agrega a la lista
                                }
                            }
                        }
                        isLoading = false
                    }
                    .addOnFailureListener { exception ->
                        errorMessage = "Error al obtener los atletas: ${exception.message}"
                        isLoading = false
                    }
            } else {
                errorMessage = "No se pudo obtener el correo del entrenador."
                isLoading = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        )
        {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando atletas...", style = MaterialTheme.typography.bodyMedium)
                }

                errorMessage != null -> {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }

                atletasList.isNotEmpty() -> {
                    Text("Atletas asociados a tu cuenta:", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))


                    atletasList.forEach { atleta ->
                        Button(
                            onClick = {

                                val intent = Intent(this@ListaAtletasActivity, DetallesAtletaActivity::class.java)
                                intent.putExtra("email", atleta.email)
                                startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = atleta.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onBackPressed()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Volver", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
