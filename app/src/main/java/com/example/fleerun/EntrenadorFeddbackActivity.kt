package com.example.fleerun

import android.app.Activity
import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class EntrenadorFeddbackActivity : ComponentActivity() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val atletaEmail = intent.getStringExtra("selectedAthlete") ?: ""

        setContent {
            MaterialTheme {
                FeedbackScreen(atletaEmail)
            }
        }
    }

    @Composable
    private fun FeedbackScreen(atletaEmail: String) {
        var feedbackItems by remember { mutableStateOf<List<StructuredFeedback>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            val dataList = mutableListOf<Pair<Long, StructuredFeedback>>()

            val querySnapshot = firestore.collection("usuarios")
                .whereEqualTo("email", atletaEmail)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents[0]
                val entrenamientosMap = doc.get("entrenamientos") as? Map<String, String> ?: emptyMap()
                val feedbacksMap = doc.get("feedbacks") as? Map<String, Any> ?: emptyMap()

                for ((fecha, detalle) in entrenamientosMap) {
                    val dateMillis = parseFechaToMillisSafe(fecha)
                    if (dateMillis != null) {
                        val feedback = feedbacksMap[fecha] as? Map<*, *> ?: emptyMap<Any, Any>()
                        val structuredFeedback = StructuredFeedback(
                            fecha = fecha,
                            detalle = detalle,
                            realizado = feedback["realizado"] as? Boolean ?: false,
                            esfuerzo = (feedback["esfuerzo"] as? Long)?.toInt(),
                            observaciones = feedback["observaciones"] as? String
                        )
                        dataList.add(dateMillis to structuredFeedback)
                    }
                }

                feedbackItems = dataList.sortedByDescending { it.first }.map { it.second }
            }

            loading = false
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(
                onClick = { (context as? Activity)?.onBackPressed() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("⬅ Volver", fontSize = 16.sp)
            }

            Text(
                text = "Feedback del Atleta",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(feedbackItems) { feedback ->
                        FeedbackCard(feedback)
                    }
                }
            }
        }
    }

    @Composable
    private fun FeedbackCard(feedback: StructuredFeedback) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📅 ${feedback.fecha}", fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "Entrenamiento:\n${feedback.detalle}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text("Realizado: ${if (feedback.realizado) "Sí" else "No"}")

                feedback.esfuerzo?.let {
                    Text("Esfuerzo: $it/10")
                }

                feedback.observaciones?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Observaciones:\n$it",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }

    private fun parseFechaToMillisSafe(fecha: String): Long? {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.parse(fecha)?.time
        } catch (e: Exception) {
            null
        }
    }

    private data class StructuredFeedback(
        val fecha: String,
        val detalle: String,
        val realizado: Boolean,
        val esfuerzo: Int?,
        val observaciones: String?
    )
}
