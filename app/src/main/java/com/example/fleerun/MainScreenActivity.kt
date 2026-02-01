package com.example.fleerun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.fleerun.ui.theme.FleeRunTheme
import com.google.firebase.auth.FirebaseAuth
import coil.compose.rememberImagePainter

class MainScreenActivity  : ComponentActivity() {

        private lateinit var auth: FirebaseAuth

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            auth = FirebaseAuth.getInstance()
            setContent {
                FleeRunTheme {
                    WelcomeScreen(auth = auth)
                }
            }
        }

        @Composable
        fun WelcomeScreen(auth: FirebaseAuth) {
            val user = auth.currentUser
            val context = LocalContext.current

            if (user == null) {
                val intent = Intent(context, LoginActivity::class.java)
                context.startActivity(intent)
                (context as? ComponentActivity)?.finish() // Finaliza la actividad actual
                return
            }

            val displayName = user.displayName ?: "Usuario"
            val profilePictureUrl = user.photoUrl?.toString() ?: "url_imagen_predeterminada"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Image(
                    painter = rememberImagePainter(profilePictureUrl),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 16.dp)
                )


                Text(
                    text = "¡Bienvenid@, $displayName!",
                    style = TextStyle(fontSize = 24.sp, color = Color.Black)
                )

                Spacer(modifier = Modifier.height(20.dp))


                Button(
                    onClick = {

                        val intent = Intent(context, TrainingActivity::class.java)
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish() // Finaliza la actividad actual para evitar que el usuario regrese a la pantalla de bienvenida
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver mis entrenamientos")
                }

                Spacer(modifier = Modifier.height(20.dp))


                Button(
                    onClick = {

                        auth.signOut()
                        val intent = Intent(context, LoginActivity::class.java)
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

