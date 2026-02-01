package com.example.fleerun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TrainingActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val email = intent.getStringExtra("selectedAthlete")

        setContent {
            MaterialTheme {
                var trainingList by remember { mutableStateOf<List<String>>(emptyList()) }
                var loading by remember { mutableStateOf(true) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(email) {
                    if (email != null) {
                        try {
                            val result = db.collection("usuarios")
                                .whereEqualTo("email", email)
                                .get()
                                .await()

                            if (result.isEmpty) {
                                errorMessage = "No se encontró el atleta con ese email."
                            } else {
                                val document = result.documents[0]
                                val entrenamientosMap = document.get("entrenamientos") as? Map<String, String> ?: emptyMap()

                                trainingList = entrenamientosMap.entries
                                    .sortedByDescending { it.key }
                                    .map { entry ->
                                        val fecha = entry.key
                                        val detalles = entry.value
                                        "Fecha: $fecha - Detalles: $detalles"
                                    }

                            }
                        } catch (e: Exception) {
                            errorMessage = "Error al cargar los entrenamientos: ${e.message}"
                        }
                    } else {
                        errorMessage = "No se encontró el atleta seleccionado."
                    }

                    loading = false
                }

                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Entrenamientos del Atleta", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (trainingList.isEmpty()) {
                            Text("No hay entrenamientos disponibles.")
                        } else {
                            trainingList.forEach { training ->
                                Button(
                                    onClick = {
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text("Entrenamiento: $training", fontSize = 18.sp)
                                }
                            }
                        }

                        errorMessage?.let {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

