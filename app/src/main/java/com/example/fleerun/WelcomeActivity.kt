package com.example.fleerun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import com.example.fleerun.ui.theme.FleeRunTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log


class WelcomeActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setContent {
            FleeRunTheme {
                WelcomeScreen(auth = auth, firestore = firestore)
            }
        }
    }

    @Composable
    fun WelcomeScreen(auth: FirebaseAuth, firestore: FirebaseFirestore) {
        val user = auth.currentUser
        val context = LocalContext.current

        if (user == null) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
            (context as? ComponentActivity)?.finish()
            return
        }

        var nameFirestore by remember { mutableStateOf("Usuario") }
        var role by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        val profilePictureUrl = user.photoUrl?.toString()

        LaunchedEffect(user.uid) {
            try {
                val docRef = firestore.collection("usuarios").document(user.uid)
                val snapshot = docRef.get().await()
                nameFirestore = snapshot.getString("name") ?: "Usuario"
                role = snapshot.getString("rol") ?: "desconocido"
                Log.d("WelcomeActivity", "Nombre de Firestore: $nameFirestore")
            } catch (e: Exception) {
                error = "Error al cargar los datos: ${e.message}"
            } finally {
                loading = false
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (error != null) {
            Text(
                text = error!!,
                color = Color.Red,
                style = TextStyle(fontSize = 16.sp)
            )
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            if (profilePictureUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(profilePictureUrl),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 16.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 16.dp)
                        .background(color = Color.Cyan, shape = CircleShape)
                ) {
                    Text(
                        text = nameFirestore.take(1),
                        style = TextStyle(fontSize = 48.sp, color = Color.White),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }


            Text(
                text = nameFirestore,
                style = TextStyle(fontSize = 24.sp, color = Color.Black)
            )


            Spacer(modifier = Modifier.height(20.dp))

            if (role == "entrenador") {
                Button(
                    onClick = {
                        val intent = Intent(context, ListaAtletasActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver mis atletas")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }


            if (role == "atleta") {
                Button(
                    onClick = {
                        val intent = Intent(context, EntrenamientosAsignadosActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver mis entrenamientos")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            
            Button(
                onClick = {
                    auth.signOut()
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                    (context as? ComponentActivity)?.finish()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}

