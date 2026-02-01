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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fleerun.ui.theme.FleeRunTheme
import com.google.firebase.firestore.FirebaseFirestore

class DetallesAtletaActivity : ComponentActivity() {

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        val atletaEmail = intent.getStringExtra("email") ?: "Email no disponible"

        setContent {
            FleeRunTheme {
                DetallesAtletaScreen(atletaEmail = atletaEmail)
            }
        }
    }

    @Composable
    fun DetallesAtletaScreen(atletaEmail: String) {
        var nombre by remember { mutableStateOf("Cargando...") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(atletaEmail) {
            firestore.collection("usuarios")
                .whereEqualTo("email", atletaEmail)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        errorMessage = "No se encontró el atleta"
                    } else {
                        val document = result.documents[0]
                        nombre = document.getString("name") ?: "Nombre no disponible"
                    }
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = "Error al obtener detalles: ${exception.message}"
                    isLoading = false
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text("Detalles del Atleta", style = TextStyle(fontSize = 24.sp))
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Cargando información del atleta...")
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, style = TextStyle(fontSize = 18.sp, color = MaterialTheme.colorScheme.error))
            }

            if (!isLoading && errorMessage == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Nombre: $nombre", fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Correo electrónico: $atletaEmail", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val intent = Intent(this@DetallesAtletaActivity, CalendarActivity::class.java)
                    intent.putExtra("atletaEmail", atletaEmail)
                    startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Añadir Entrenamiento", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val intent = Intent(this@DetallesAtletaActivity, FeedbackActivity::class.java)
                    intent.putExtra("atletaEmail", atletaEmail)
                    startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver Feedback del Atleta", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { finish() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver", fontSize = 18.sp)
            }
        }
    }
}
