package com.example.fleerun

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

class FeedbackActivity : ComponentActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        val atletaEmail = intent.getStringExtra("atletaEmail") ?: ""
        val entrenadorEmail = intent.getStringExtra("entrenadorEmail") ?: ""

        setContent {
            MaterialTheme {
                FeedbackScreen(atletaEmail, entrenadorEmail)
            }
        }
    }

    @Composable
    fun FeedbackScreen(atletaEmail: String, entrenadorEmail: String) {
        val context = LocalContext.current

        var entrenamientosList by remember { mutableStateOf<List<String>>(emptyList()) }
        var feedbacks by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var feedbackText by remember { mutableStateOf("") }
        var selectedFecha by remember { mutableStateOf<String?>(null) }

        val esAtleta = entrenadorEmail.isEmpty() || entrenadorEmail == atletaEmail

        LaunchedEffect(atletaEmail) {
            try {
                val result = firestore.collection("usuarios")
                    .whereEqualTo("email", atletaEmail)
                    .get()
                    .await()

                if (result.isEmpty) {
                    errorMessage = "No se encontró el atleta"
                } else {
                    val document = result.documents[0]
                    val entrenamientos = document.get("entrenamientos") as? Map<String, String> ?: emptyMap()
                    val feedbacksMap = document.get("feedbacks") as? Map<String, Map<String, Any>> ?: emptyMap()
                    val feedbacksTemp = mutableMapOf<String, String>()

                    // Obtener la lista de fechas y ordenarlas por el timestamp del feedback
                    val sortedEntrenamientos = entrenamientos.keys.sortedByDescending { fecha ->
                        val feedbackInfo = feedbacksMap[fecha]
                        val timestamp = feedbackInfo?.get("fecha") as? Long
                        timestamp ?: 0L
                    }

                    entrenamientosList = sortedEntrenamientos

                    for (fecha in sortedEntrenamientos) {
                        val entrenamientoTexto = entrenamientos[fecha] ?: "Sin entrenamiento registrado"
                        val feedbackInfo = feedbacksMap[fecha]

                        if (feedbackInfo == null) {
                            feedbacksTemp[fecha] = "Entrenamiento: $entrenamientoTexto\nSin feedback"
                        } else {
                            val realizado = when (feedbackInfo["realizado"]) {
                                true, "true" -> "Hecho"
                                false, "false" -> "No hecho"
                                else -> "No especificado"
                            }
                            val esfuerzo = feedbackInfo["esfuerzo"]?.toString() ?: "Sin puntuación"
                            val observaciones = feedbackInfo["observaciones"]?.toString() ?: "Sin observaciones"

                            val fbText = """
                                Entrenamiento: $entrenamientoTexto
                                Completado: $realizado
                                Puntuación: $esfuerzo
                                Observación: $observaciones
                            """.trimIndent()

                            feedbacksTemp[fecha] = fbText
                        }
                    }

                    feedbacks = feedbacksTemp
                }
            } catch (e: Exception) {
                errorMessage = "Error al obtener feedback: ${e.message}"
            }
        }

        // UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text("Feedback del Atleta", style = TextStyle(fontSize = 24.sp))
            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entrenamientosList) { entrenamiento ->
                    val feedback = feedbacks[entrenamiento] ?: "Cargando feedback..."
                    var isExpanded by remember { mutableStateOf(false) }

                    Button(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entrenamiento: $entrenamiento", fontSize = 18.sp)
                    }

                    if (isExpanded) {
                        Text(
                            text = feedback,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 12.dp)
                        )

                        if (esAtleta) {
                            Spacer(modifier = Modifier.height(8.dp))

                            if (selectedFecha == entrenamiento) {
                                TextField(
                                    value = feedbackText,
                                    onValueChange = { feedbackText = it },
                                    label = { Text("Escribe tu feedback") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        selectedFecha?.let { fecha ->
                                            guardarFeedbackNuevo(
                                                fecha,
                                                feedbackText,
                                                atletaEmail,
                                                context
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Guardar Feedback", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun guardarFeedbackNuevo(
        fecha: String,
        feedbackText: String,
        atletaEmail: String,
        context: android.content.Context
    ) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        val feedbackMap = hashMapOf(
            "realizado" to true,
            "esfuerzo" to 5,
            "observaciones" to feedbackText,
            "fecha" to System.currentTimeMillis(),
            "atletaEmail" to atletaEmail
        )

        firestore.collection("usuarios")
            .whereEqualTo("email", atletaEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.documents[0]
                    val userId = userDoc.id
                    val docRef = firestore.collection("usuarios").document(userId)

                    docRef.update("feedbacks.$fecha", feedbackMap)
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Feedback guardado correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(
                                context,
                                "Error al guardar feedback: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(context, "No se encontró el usuario", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
