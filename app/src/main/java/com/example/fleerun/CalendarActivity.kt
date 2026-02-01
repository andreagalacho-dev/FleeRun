package com.example.fleerun

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val atletaEmail = intent.getStringExtra("atletaEmail") ?: ""

        setContent {
            MaterialTheme {
                CalendarScreen(atletaEmail)
            }
        }
    }

    @Composable
    fun CalendarScreen(atletaEmail: String) {
        val context = LocalContext.current
        var showDatePicker by remember { mutableStateOf(false) }
        var selectedDate by remember { mutableStateOf<String?>(null) }
        var entrenamiento by remember { mutableStateOf("") }
        var entrenamientosMap by remember { mutableStateOf(mutableMapOf<String, String>()) }
        var editandoFecha by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(atletaEmail) {
            if (atletaEmail.isNotEmpty()) {
                firestore.collection("usuarios")
                    .whereEqualTo("email", atletaEmail)
                    .get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            val document = result.documents[0]
                            val entrenos = document.get("entrenamientos") as? Map<String, String> ?: emptyMap()
                            entrenamientosMap = entrenos.toMutableMap()
                        }
                    }
            }
        }

        if (showDatePicker) {
            LaunchedEffect(Unit) {
                val calendar = Calendar.getInstance()
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val fecha = String.format("%02d/%02d/%04d", d, m + 1, y)
                        selectedDate = fecha
                        showDatePicker = false
                    },
                    year, month, day
                ).show()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { onBackPressed() },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("Volver a la pantalla anterior")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Añadir Entrenamiento", fontSize = 22.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { showDatePicker = true }) {
                Text("Elegir Fecha")
            }

            selectedDate?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Fecha seleccionada: $it")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = entrenamiento,
                onValueChange = { entrenamiento = it },
                label = { Text("Entrenamiento") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val fecha = selectedDate
                    if (fecha.isNullOrBlank() || entrenamiento.isBlank()) {
                        Toast.makeText(context, "Debes seleccionar fecha y escribir entrenamiento", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Actualizar en Firestore
                    firestore.collection("usuarios")
                        .whereEqualTo("email", atletaEmail)
                        .get()
                        .addOnSuccessListener { result ->
                            if (!result.isEmpty) {
                                val docRef = result.documents[0].reference
                                entrenamientosMap[fecha] = entrenamiento

                                docRef.set(
                                    mapOf("entrenamientos" to entrenamientosMap),
                                    SetOptions.merge()
                                ).addOnSuccessListener {
                                    Toast.makeText(context, "✅ Guardado", Toast.LENGTH_SHORT).show()
                                    entrenamiento = ""
                                    selectedDate = null
                                    editandoFecha = null
                                }
                            }
                        }
                }
            ) {
                Text(if (editandoFecha != null) "Actualizar" else "Guardar")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Entrenamientos Guardados", fontSize = 18.sp)

            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                val listaOrdenada = entrenamientosMap.toList().sortedByDescending {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(it.first)
                }

                items(listaOrdenada) { (fecha, detalle) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("📅 $fecha", fontSize = 16.sp)
                            Text("🏋️ $detalle", fontSize = 16.sp)
                        }


                        Row {
                            Button(onClick = {
                                selectedDate = fecha
                                entrenamiento = detalle
                                editandoFecha = fecha
                            }) {
                                Text("Editar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                firestore.collection("usuarios")
                                    .whereEqualTo("email", atletaEmail)
                                    .get()
                                    .addOnSuccessListener { result ->
                                        if (!result.isEmpty) {
                                            val docRef = result.documents[0].reference
                                            entrenamientosMap.remove(fecha)
                                            docRef.set(
                                                mapOf("entrenamientos" to entrenamientosMap),
                                                SetOptions.merge()
                                            ).addOnSuccessListener {
                                                Toast.makeText(context, "Borrado", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                            }) {
                                Text("Borrar")
                            }
                        }
                    }
                }
            }
        }
    }
}
