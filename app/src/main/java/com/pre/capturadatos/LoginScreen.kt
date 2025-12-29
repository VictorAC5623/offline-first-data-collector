package com.pre.capturadatos

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.pre.capturadatos.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen() {

    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val auth = remember { FirebaseAuth.getInstance() }

    // 1. Configurar el cliente de Google Sign-In
    // Pide el 'Web Client ID' que está en tu nuevo google-services.json
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // 2. Crear el 'launcher' que recibe el resultado del pop-up de Google
    val authResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Google Sign-In fue exitoso, ahora autenticamos con Firebase
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken!!
                val credential = GoogleAuthProvider.getCredential(idToken, null)

                // Autenticar en Firebase
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { firebaseTask ->
                        isLoading = false
                        if (firebaseTask.isSuccessful) {
                            // ¡Éxito! El AuthStateListener de MainActivity lo detectará
                            // y navegará a la DataCaptureScreen.
                            errorMessage = null
                        } else {
                            errorMessage = "Error de Firebase: ${firebaseTask.exception?.message}"
                        }
                    }
            } catch (e: ApiException) {
                // Error al obtener la cuenta de Google
                isLoading = false
                errorMessage = "Error de Google: ${e.statusCode}"
                Log.e("GoogleSignIn", "ApiException: ${e.statusCode}")
            }
        } else {
            // El usuario cerró el pop-up de Google
            isLoading = false
            errorMessage = "Inicio de sesión cancelado."
        }
    }

    // 3. La Interfaz de Usuario (UI)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Mantenemos el logo que añadiste
        Image(
            painter = painterResource(id = R.drawable.logo_preu),
            contentDescription = "Logo del Preuniversitario",
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(bottom = 32.dp)
        )

        Text(
            text = "Inicio de Sesión",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. El nuevo botón de "Ingresar con Google"
        Button(
            onClick = {
                isLoading = true
                errorMessage = null
                // Lanzamos el pop-up de Google
                authResultLauncher.launch(googleSignInClient.signInIntent)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            // (Opcional: puedes añadir un icono de Google aquí)
            Text(if (isLoading) "Conectando..." else "Ingresar con Google")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar indicador de carga
        if (isLoading) {
            CircularProgressIndicator()
        }

        // Mostrar mensaje de error (si lo hay)
        val currentError = errorMessage
        if (currentError != null) {
            Text(
                text = currentError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}