package com.example.fleerun

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class EntrenamientosAsignadosActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                EntrenamientosAsignadosScreen()
            }
        }
    }

    @Composable
    fun EntrenamientosAsignadosScreen() {
        val coroutineScope = rememberCoroutineScope()
        var entrenamientos by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
        var feedbacks by remember { mutableStateOf<Map<String, Map<String, Any>>>(emptyMap()) }
        var loading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val user = auth.currentUser
        val atletaEmail = user?.email ?: ""

        LaunchedEffect(Unit) {
            try {
                val querySnapshot = firestore.collection("usuarios")
                    .whereEqualTo("email", atletaEmail)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val entrenamientosMap = document.get("entrenamientos") as? Map<String, String>
                    entrenamientos = entrenamientosMap
                        ?.map { it.key to it.value }
                        ?.sortedByDescending { (fecha, _) -> parseFecha(fecha) } ?: emptyList()

                    val feedbacksMap = document.get("feedbacks") as? Map<String, Map<String, Any>>
                    feedbacks = feedbacksMap ?: emptyMap()
                } else {
                    errorMessage = "No se encontró información del atleta."
                }
            } catch (e: Exception) {
                errorMessage = "Error al cargar entrenamientos: ${e.message}"
            }
            loading = false
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Text("Mis Entrenamientos", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
                items(entrenamientos) { (fecha, detalle) ->
                    val feedback = feedbacks[fecha]
                    var showForm by remember { mutableStateOf(false) }
                    var esfuerzo by remember { mutableStateOf((feedback?.get("esfuerzo") as? Long)?.toFloat() ?: 5f) }
                    var observaciones by remember { mutableStateOf(feedback?.get("observaciones") as? String ?: "") }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📅 $fecha", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(detalle)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Si el feedback existe, mostrar detalles del mismo
                            if (feedback != null) {
                                Text("✅ Feedback enviado", color = MaterialTheme.colorScheme.primary)
                                Text("Realizado: ${if (feedback["realizado"] == true) "Hecho" else "No realizado"}")
                                Text("Esfuerzo: ${feedback["esfuerzo"]}")
                                Text("Observaciones:\n${feedback["observaciones"]}", modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { showForm = !showForm }) {
                                    Text(if (showForm) "Cancelar edición" else "Editar feedback")
                                }
                            } else {
                                // Botón de "Entrenamiento no realizado"
                                Button(onClick = {
                                    // Marcar como no realizado
                                    coroutineScope.launch {
                                        sendFeedbackToFirestore(
                                            atletaEmail,
                                            fecha,
                                            realizado = false,
                                            esfuerzo = null,
                                            observaciones = "Entrenamiento no realizado"
                                        )
                                        val updated = firestore.collection("usuarios")
                                            .whereEqualTo("email", atletaEmail)
                                            .get().await()
                                        feedbacks = updated.documents[0].get("feedbacks") as? Map<String, Map<String, Any>> ?: emptyMap()
                                    }
                                }) {
                                    Text("Entrenamiento no realizado")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(onClick = { showForm = true }) {
                                    Text("Entrenamiento realizado")
                                }
                            }


                            if (showForm) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Nivel de esfuerzo: ${esfuerzo.toInt()}")
                                Slider(
                                    value = esfuerzo,
                                    onValueChange = { esfuerzo = it },
                                    valueRange = 1f..10f,
                                    steps = 8
                                )
                                OutlinedTextField(
                                    value = observaciones,
                                    onValueChange = { observaciones = it },
                                    label = { Text("Observaciones") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    coroutineScope.launch {
                                        sendFeedbackToFirestore(
                                            atletaEmail,
                                            fecha,
                                            realizado = true,
                                            esfuerzo = esfuerzo.toInt(),
                                            observaciones = observaciones
                                        )
                                        showForm = false

                                        val updated = firestore.collection("usuarios")
                                            .whereEqualTo("email", atletaEmail)
                                            .get().await()
                                        feedbacks = updated.documents[0].get("feedbacks") as? Map<String, Map<String, Any>> ?: emptyMap()
                                    }
                                }) {
                                    Text("Guardar feedback")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun sendFeedbackToFirestore(
        atletaEmail: String,
        fecha: String,
        realizado: Boolean,
        esfuerzo: Int?,
        observaciones: String?
    ) {
        try {
            val query = firestore.collection("usuarios")
                .whereEqualTo("email", atletaEmail)
                .get()
                .await()

            if (!query.isEmpty) {
                val userDoc = query.documents[0]
                val docRef = firestore.collection("usuarios").document(userDoc.id)

                val feedbackData = mutableMapOf<String, Any>(
                    "realizado" to realizado  // Indicamos si fue realizado o no
                )

                if (esfuerzo != null) feedbackData["esfuerzo"] = esfuerzo
                if (observaciones != null) feedbackData["observaciones"] = observaciones

                docRef.set(
                    mapOf("feedbacks" to mapOf(fecha to feedbackData)),
                    SetOptions.merge()
                ).await()
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error al guardar feedback: ${e.message}")
        }
    }

    private fun parseFecha(fecha: String): Date? {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formato.parse(fecha)
        } catch (e: Exception) {
            null
        }
    }
}
