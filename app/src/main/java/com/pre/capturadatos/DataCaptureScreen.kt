package com.pre.capturadatos

import android.media.MediaPlayer // <-- Para el sonido
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // <-- Para el sonido
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
//import com.pre.capturadatos.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCaptureScreen(onLogout: () -> Unit) {

    val db = remember { Firebase.firestore }
    val errorColor = MaterialTheme.colorScheme.error
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current // <-- Necesario para reproducir el sonido

    // --- Estados ---
    var nombre by rememberSaveable { mutableStateOf("") }
    var apellidos by rememberSaveable { mutableStateOf("") }
    var colegio by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var selectedAno by rememberSaveable { mutableStateOf("") }
    var selectedPreu by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var saveStatusMessage by remember { mutableStateOf<Pair<String, Color>?>(null) }
    var isPhoneValid by remember { mutableStateOf(false) }

    // --- Listas y Lógica ---
    val anos = listOf("Primero", "Segundo", "Tercero", "Graduado")
    val preuOptions = mapOf(true to "Sí", false to "No")

    LaunchedEffect(telefono) {
        isPhoneValid = telefono.length == 10 && telefono.all { it.isDigit() }
    }

    val isFormReady = nombre.isNotBlank() &&
            apellidos.isNotBlank() &&
            colegio.isNotBlank() &&
            selectedAno.isNotBlank() &&
            selectedPreu != null &&
            isPhoneValid &&
            !isLoading

    LaunchedEffect(saveStatusMessage) {
        if (saveStatusMessage != null) {
            delay(3000)
            saveStatusMessage = null
        }
    }

    fun clearForm() {
        nombre = ""
        apellidos = ""
        colegio = ""
        telefono = ""
        selectedAno = ""
        selectedPreu = null
        focusManager.clearFocus()
    }

    // --- Función para reproducir sonido ---
    fun playSuccessSound() {
        try {
            // Crea y reproduce el sonido 'success_sound' desde la carpeta raw
            val mediaPlayer = MediaPlayer.create(context, R.raw.success_sound)
            mediaPlayer.setOnCompletionListener { mp -> mp.release() } // Libera memoria al terminar
            mediaPlayer.start()
        } catch (e: Exception) {
            // Si falla el sonido, no pasa nada, la app sigue
            e.printStackTrace()
        }
    }

    // --- UI ---
    Scaffold(
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            Image(
                painter = painterResource(id = R.drawable.logo_preu),
                contentDescription = "Logo de fondo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.1f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- CAMBIO 1: Cabecera LIMPIA (Sin botón de cerrar sesión) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center, // Centrado
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Registro",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))

                // --- Campos de Texto ---

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apellidos,
                    onValueChange = { apellidos = it },
                    label = { Text("Apellidos") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = colegio,
                    onValueChange = { colegio = it },
                    label = { Text("Colegio") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                // --- CAMBIO 2: Teléfono con auto-ocultar teclado ---
                OutlinedTextField(
                    value = telefono,
                    onValueChange = {
                        if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                            telefono = it
                            // Si llega a 10 dígitos, esconde el teclado automáticamente
                            if (it.length == 10) {
                                focusManager.clearFocus()
                            }
                        }
                    },
                    label = { Text("Teléfono (10 dígitos)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = true,
                    isError = !isPhoneValid && telefono.isNotBlank(),
                    supportingText = {
                        if (!isPhoneValid && telefono.isNotBlank()) {
                            Text("Debe tener 10 dígitos")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Año de curso:", style = MaterialTheme.typography.bodyLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    anos.forEach { ano ->
                        FilterChip(
                            selected = selectedAno == ano,
                            onClick = { selectedAno = ano },
                            label = { Text(ano) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text("¿Inscrito en otro preuniversitario?", style = MaterialTheme.typography.bodyLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    preuOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = selectedPreu == value,
                            onClick = { selectedPreu = value },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (isFormReady) {
                            isLoading = true
                            saveStatusMessage = null
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            try {
                                val prospecto = hashMapOf(
                                    "nombre" to nombre.trim(),
                                    "apellidos" to apellidos.trim(),
                                    "colegio" to colegio.trim(),
                                    "telefono" to telefono,
                                    "ano_curso" to selectedAno,
                                    "inscrito_otro_preu" to selectedPreu,
                                    "fecha_registro" to FieldValue.serverTimestamp(),
                                    "staff_uid" to currentUser?.uid,
                                    "staff_email" to currentUser?.email
                                )
                                db.collection("prospectos").add(prospecto)

                                // --- CAMBIO 3: Sonido de Éxito ---
                                playSuccessSound()

                                saveStatusMessage = Pair("¡Guardado con éxito!", Color(0xFF008000))
                                clearForm()
                            } catch (e: Exception) {
                                saveStatusMessage = Pair("Error local al guardar: ${e.message}", errorColor)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = isFormReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (isLoading) "Enviando..." else "Enviar",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                val currentMessage = saveStatusMessage
                if (currentMessage != null) {
                    Text(
                        text = currentMessage.first,
                        color = currentMessage.second,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp)) // Espacio separador

                // --- CAMBIO 1 (Parte 2): Botón de Cerrar Sesión MOVIDO AL FINAL ---
                TextButton(
                    onClick = onLogout,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        "Cerrar Sesión (Solo Staff)",
                        color = Color.Gray, // Color discreto
                        style = MaterialTheme.typography.bodySmall
                    )
                }

            } // Fin Column
        } // Fin Box
    } // Fin Scaffold
}