package com.pre.capturadatos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestoreSettings // <-- NUEVO IMPORT
import com.google.firebase.firestore.PersistentCacheSettings // <-- NUEVO IMPORT
import com.google.firebase.firestore.ktx.firestore
// Borraremos los imports '...ktx.firestoreSettings' y '...ktx.persistentCacheSettings' si siguen ahí
import com.google.firebase.ktx.Firebase
import com.pre.capturadatos.ui.theme.PreuCapturaAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- INICIO DE LA CONFIGURACIÓN OFFLINE (SINTAXIS 2025) ---
        // 1. Obtenemos la instancia de Firestore
        val db = Firebase.firestore

        // 2. Usamos el Builder manual (esta NO está obsoleta)
        //    Iniciamos el constructor con la configuración existente de 'db'
        val settings = FirebaseFirestoreSettings.Builder(db.firestoreSettings)
            // 3. Habilitamos el cache persistente (la nueva forma)
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    // Usamos el constructor vacío para el tamaño por defecto (ilimitado)
                    .build() // <-- Ajuste de comentario: 'ilimitado' es correcto en español
            )
            .build()

        // 4. Aplicamos la configuración
        db.firestoreSettings = settings
        // --- FIN DE LA CONFIGURACIÓN OFFLINE ---


        // Inicializamos Firebase Auth (tu código original)
        auth = Firebase.auth

        setContent {
            PreuCapturaAppTheme {

                var isLoggedIn by remember { mutableStateOf(false) }

                DisposableEffect(auth) {
                    val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        isLoggedIn = (firebaseAuth.currentUser != null)
                    }
                    auth.addAuthStateListener(authListener)
                    onDispose {
                        auth.removeAuthStateListener(authListener)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoggedIn) {
                        DataCaptureScreen(
                            onLogout = {
                                auth.signOut()
                            }
                        )
                    } else {
                        LoginScreen()
                    }
                }
            }
        }
    }
}