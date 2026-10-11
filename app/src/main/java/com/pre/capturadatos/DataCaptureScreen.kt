package com.pre.capturadatos

import android.media.MediaPlayer // <-- Para el sonido
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // <-- Para el sonido
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VERDE = Color(0xFF008000)
private val NARANJA = Color(0xFFB45309)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCaptureScreen(onLogout: () -> Unit) {

    val errorColor = MaterialTheme.colorScheme.error
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current // <-- Necesario para reproducir el sonido
    val store = remember { CapturaStore.get(context) }
    val nombreFocus = remember { FocusRequester() }

    val pendientes by store.pendientes.collectAsState()
    val ultimaSyncMs by store.ultimaSyncMs.collectAsState()
    val registros by store.registros.collectAsState()

    LaunchedEffect(Unit) { store.reconciliar() }

    // --- Estados ---
    var nombre by rememberSaveable { mutableStateOf("") }
    var apellidos by rememberSaveable { mutableStateOf("") }
    var colegio by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var selectedAno by rememberSaveable { mutableStateOf("") }
    var selectedPreu by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var saveStatusMessage by remember { mutableStateOf<Pair<String, Color>?>(null) }
    var mostrarQr by remember { mutableStateOf(false) }
    var mostrarStaff by remember { mutableStateOf(false) }

    // --- Listas y Lógica ---
    val anos = listOf("Primero", "Segundo", "Tercero", "Graduado")
    val preuOptions = mapOf(true to "Sí", false to "No")

    // Celulares de Ecuador: 10 dígitos empezando por 09 (es el número al que escribe el bot).
    val isPhoneValid = telefono.length == 10 && telefono.startsWith("09") && telefono.all { it.isDigit() }
    val yaRegistrado = remember(telefono, registros) { store.yaRegistrado(telefono) }
    val sugerenciasColegio = remember(colegio, registros) { store.colegiosFrecuentes(colegio) }
    val registrosHoy = remember(registros) { store.registrosDeHoy() }

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

                EstadoSincronizacion(pendientes, ultimaSyncMs, registrosHoy)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Registro",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    OutlinedButton(onClick = { mostrarQr = true }) {
                        Text("QR WhatsApp")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // --- Campos de Texto ---

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nombreFocus),
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
                if (sugerenciasColegio.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sugerenciasColegio.forEach { sugerencia ->
                            SuggestionChip(
                                onClick = {
                                    colegio = sugerencia
                                    focusManager.moveFocus(FocusDirection.Down)
                                },
                                label = { Text(sugerencia) }
                            )
                        }
                    }
                }
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
                        when {
                            telefono.isNotBlank() && !isPhoneValid ->
                                Text("Debe empezar con 09 y tener 10 dígitos")
                            yaRegistrado ->
                                Text("Este número ya se registró en este celular", color = NARANJA)
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
                            try {
                                store.guardar(
                                    NuevoProspecto(
                                        nombre = nombre.trim(),
                                        apellidos = apellidos.trim(),
                                        colegio = colegio.trim(),
                                        telefono = telefono,
                                        anoCurso = selectedAno,
                                        inscritoOtroPreu = selectedPreu == true,
                                    ),
                                    FirebaseAuth.getInstance().currentUser
                                )

                                // --- CAMBIO 3: Sonido de Éxito ---
                                playSuccessSound()

                                saveStatusMessage = Pair("¡Guardado con éxito!", VERDE)
                                clearForm()
                                // Listo para el siguiente visitante sin tocar la pantalla.
                                nombreFocus.requestFocus()
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

                Text(
                    text = StandConfig.CONSENTIMIENTO_TEXTO,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                val currentMessage = saveStatusMessage
                if (currentMessage != null) {
                    Text(
                        text = currentMessage.first,
                        color = currentMessage.second,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp)) // Espacio separador

                TextButton(
                    onClick = { mostrarStaff = true },
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        "Opciones de staff",
                        color = Color.Gray, // Color discreto
                        style = MaterialTheme.typography.bodySmall
                    )
                }

            } // Fin Column
        } // Fin Box
    } // Fin Scaffold

    if (mostrarQr) {
        DialogoQr(onCerrar = { mostrarQr = false })
    }
    if (mostrarStaff) {
        DialogoStaff(
            pendientes = pendientes,
            store = store,
            onLogout = onLogout,
            onCerrar = { mostrarStaff = false }
        )
    }
}

@Composable
private fun EstadoSincronizacion(pendientes: Int, ultimaSyncMs: Long, registrosHoy: Int) {
    val (texto, color) = if (pendientes > 0) {
        "⏳ $pendientes ${if (pendientes == 1) "registro pendiente" else "registros pendientes"} de subir" to NARANJA
    } else {
        val hora = if (ultimaSyncMs > 0) " · " + SimpleDateFormat("HH:mm", Locale.US).format(Date(ultimaSyncMs)) else ""
        "✓ Todo sincronizado$hora" to VERDE
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, color = color, style = MaterialTheme.typography.bodyMedium)
        Text("Hoy: $registrosHoy", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DialogoQr(onCerrar: () -> Unit) {
    val enlace = remember { StandConfig.enlaceWhatsApp() }
    val qr = remember(enlace) { generarQr(enlace) }
    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
        title = { Text("Escanea y escríbenos", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = qr,
                    contentDescription = "Código QR de WhatsApp",
                    modifier = Modifier.size(260.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Abre la cámara, escanea el código y envía el mensaje: te respondemos con tu descuento de feria.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

@Composable
private fun DialogoStaff(
    pendientes: Int,
    store: CapturaStore,
    onLogout: () -> Unit,
    onCerrar: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val pinGuardado = remember { ModoStand.pin(context) }
    var desbloqueado by remember { mutableStateOf(pinGuardado == null) }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var standActivo by remember { mutableStateOf(ModoStand.activo(context)) }
    var nuevoPin by remember { mutableStateOf("") }
    var aviso by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
        title = { Text("Opciones de staff") },
        text = {
            if (!desbloqueado) {
                Column {
                    Text("Ingresa el PIN del staff")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it; pinError = false },
                        singleLine = true,
                        isError = pinError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        supportingText = { if (pinError) Text("PIN incorrecto") }
                    )
                    Button(onClick = {
                        if (pin == pinGuardado) desbloqueado = true else pinError = true
                    }) { Text("Entrar") }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // --- Modo Stand ---
                    if (ModoStand.pin(context) == null) {
                        Text("Para usar el modo Stand primero crea un PIN (4 a 6 dígitos):")
                        OutlinedTextField(
                            value = nuevoPin,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) nuevoPin = it },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )
                        Button(
                            enabled = nuevoPin.length >= 4,
                            onClick = { ModoStand.guardarPin(context, nuevoPin); aviso = "PIN guardado" }
                        ) { Text("Guardar PIN") }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Modo Stand (app fija en pantalla)", modifier = Modifier.weight(1f))
                            Switch(
                                checked = standActivo,
                                onCheckedChange = { activar ->
                                    activity?.let { if (activar) ModoStand.activar(it) else ModoStand.desactivar(it) }
                                    standActivo = activar
                                }
                            )
                        }
                    }

                    // --- Respaldo ---
                    OutlinedButton(
                        onClick = {
                            val intent = store.intentExportarRespaldo()
                            if (intent == null) aviso = "Todavía no hay registros en este celular"
                            else context.startActivity(android.content.Intent.createChooser(intent, "Enviar respaldo"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Exportar respaldo (CSV)") }

                    // --- Cerrar sesión ---
                    // Con registros sin subir, cerrar sesión los deja atrapados en el celular hasta
                    // que vuelva a entrar la misma cuenta: se bloquea en vez de solo advertir.
                    if (pendientes > 0) {
                        Text(
                            "No se puede cerrar sesión: hay $pendientes " +
                                (if (pendientes == 1) "registro" else "registros") + " sin subir. " +
                                "Conecta el celular a internet y espera a que diga \"Todo sincronizado\".",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(
                        enabled = pendientes == 0 && !standActivo,
                        onClick = { onCerrar(); onLogout() }
                    ) { Text("Cerrar sesión") }
                    if (standActivo) {
                        Text(
                            "Desactiva el modo Stand para cerrar sesión.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    aviso?.let { Text(it, color = VERDE, style = MaterialTheme.typography.bodySmall) }
                    Text(
                        "Versión ${BuildConfig.VERSION_NAME} · equipo ${store.deviceId.take(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    )
}
