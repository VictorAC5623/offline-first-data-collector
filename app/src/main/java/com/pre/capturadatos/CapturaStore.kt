package com.pre.capturadatos

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object StandConfig {
    const val WHATSAPP_NUMERO = "593995412463"
    const val CONSENTIMIENTO_VERSION = "v1-2026-10"
    const val CONSENTIMIENTO_TEXTO =
        "Al registrarte aceptas que Preuniversitario Newman use tus datos para contactarte " +
            "con información de sus cursos."

    // El codigo lleva la fecha del dia: el bot lo traduce al evento de esa fecha
    // (nunca hay dos ferias el mismo dia), asi no hay que publicar un APK por feria.
    fun codigoStand(fecha: Date = Date()): String =
        "STAND-" + SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("America/Guayaquil")
        }.format(fecha)

    fun enlaceWhatsApp(): String {
        val texto = "Hola, vengo del stand de Newman 🎓 (código ${codigoStand()})"
        return "https://wa.me/$WHATSAPP_NUMERO?text=" +
            URLEncoder.encode(texto, "UTF-8").replace("+", "%20")
    }
}

data class NuevoProspecto(
    val nombre: String,
    val apellidos: String,
    val colegio: String,
    val telefono: String,
    val anoCurso: String,
    val inscritoOtroPreu: Boolean,
)

data class RegistroLocal(
    val id: String,
    val capturadoEnMs: Long,
    val colegio: String,
    val telefono: String,
)

/**
 * Todo lo que la app guarda pasa por aqui.
 *
 * Orden de escritura: primero el respaldo CSV en el telefono, despues Firestore (que tiene
 * su propia cache offline). Si la sincronizacion fallara en plena feria, el CSV se puede
 * exportar desde las opciones de staff.
 */
class CapturaStore private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("captura", Context.MODE_PRIVATE)
    private val db = Firebase.firestore
    private val respaldo = File(appContext.filesDir, "respaldo_capturas.csv")

    private val _pendientes = MutableStateFlow(prefs.getInt(K_PENDIENTES, 0))
    val pendientes: StateFlow<Int> = _pendientes

    private val _ultimaSyncMs = MutableStateFlow(prefs.getLong(K_ULTIMA_SYNC, 0L))
    val ultimaSyncMs: StateFlow<Long> = _ultimaSyncMs

    private val _registros = MutableStateFlow(leerRespaldo())
    val registros: StateFlow<List<RegistroLocal>> = _registros

    val deviceId: String =
        prefs.getString(K_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString(K_DEVICE_ID, it).apply() }

    fun guardar(p: NuevoProspecto, staff: FirebaseUser?): String {
        val id = UUID.randomUUID().toString()
        val ahora = Date()

        agregarAlRespaldo(id, ahora, p, staff?.email)

        val doc = hashMapOf(
            "nombre" to p.nombre,
            "apellidos" to p.apellidos,
            "colegio" to p.colegio,
            "telefono" to p.telefono,
            "ano_curso" to p.anoCurso,
            "inscrito_otro_preu" to p.inscritoOtroPreu,
            "fecha_registro" to FieldValue.serverTimestamp(),
            "capturado_en" to Timestamp(ahora),
            "staff_uid" to staff?.uid,
            "staff_email" to staff?.email,
            "device_id" to deviceId,
            "app_version" to BuildConfig.VERSION_NAME,
            "consentimiento" to StandConfig.CONSENTIMIENTO_VERSION,
        )

        sumarPendientes(1)
        // set() con ID propio: si el envio se repite, sobrescribe el mismo documento en vez de duplicar.
        // El listener solo dispara cuando el servidor confirma, no cuando se guarda en la cache.
        db.collection("prospectos").document(id).set(doc)
            .addOnSuccessListener {
                sumarPendientes(-1)
                marcarSincronizado()
            }
        return id
    }

    /**
     * Si la app se cerro con registros sin confirmar, sus listeners se perdieron.
     * waitForPendingWrites termina cuando la cache ya no tiene nada por subir.
     */
    fun reconciliar() {
        db.waitForPendingWrites().addOnSuccessListener {
            _pendientes.value = 0
            prefs.edit().putInt(K_PENDIENTES, 0).apply()
            marcarSincronizado()
        }
    }

    fun registrosDeHoy(): Int {
        val hoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return _registros.value.count { it.capturadoEnMs >= hoy }
    }

    fun yaRegistrado(telefono: String): Boolean =
        telefono.length == 10 && _registros.value.any { it.telefono == telefono }

    // En una feria dentro de un colegio casi todos escriben el mismo nombre: se ofrece con un toque.
    fun colegiosFrecuentes(prefijo: String, max: Int = 4): List<String> {
        val p = prefijo.trim()
        return _registros.value.takeLast(300)
            .map { it.colegio.trim() }
            .filter { it.isNotBlank() && it.startsWith(p, ignoreCase = true) && !it.equals(p, ignoreCase = true) }
            .groupBy { it.lowercase() }
            .values.sortedByDescending { it.size }
            .map { it.last() }
            .take(max)
    }

    fun intentExportarRespaldo(): Intent? {
        if (!respaldo.exists()) return null
        val dir = File(appContext.cacheDir, "exportes").apply { mkdirs() }
        val nombre = "respaldo_${deviceId.take(8)}_" +
            SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".csv"
        val copia = File(dir, nombre)
        respaldo.copyTo(copia, overwrite = true)
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.archivos", copia)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Respaldo Newman-Registro $nombre")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // --- Internos ---

    private fun sumarPendientes(delta: Int) {
        val nuevo = (_pendientes.value + delta).coerceAtLeast(0)
        _pendientes.value = nuevo
        prefs.edit().putInt(K_PENDIENTES, nuevo).apply()
    }

    private fun marcarSincronizado() {
        val ahora = System.currentTimeMillis()
        _ultimaSyncMs.value = ahora
        prefs.edit().putLong(K_ULTIMA_SYNC, ahora).apply()
    }

    private fun agregarAlRespaldo(id: String, cuando: Date, p: NuevoProspecto, staffEmail: String?) {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(cuando)
        val nuevo = !respaldo.exists()
        respaldo.appendText(buildString {
            if (nuevo) appendLine(CABECERA.joinToString(","))
            appendLine(
                listOf(
                    id, iso, p.nombre, p.apellidos, p.colegio, p.telefono, p.anoCurso,
                    if (p.inscritoOtroPreu) "si" else "no", staffEmail.orEmpty(), deviceId,
                ).joinToString(",") { csv(it) }
            )
        })
        _registros.value = _registros.value + RegistroLocal(id, cuando.time, p.colegio, p.telefono)
    }

    private fun leerRespaldo(): List<RegistroLocal> {
        if (!respaldo.exists()) return emptyList()
        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        return respaldo.readLines().drop(1).mapNotNull { linea ->
            val c = parsearCsv(linea)
            if (c.size < 6) return@mapNotNull null
            val ms = runCatching { formato.parse(c[1])?.time }.getOrNull() ?: return@mapNotNull null
            RegistroLocal(id = c[0], capturadoEnMs = ms, colegio = c[4], telefono = c[5])
        }
    }

    companion object {
        private const val K_PENDIENTES = "pendientes"
        private const val K_ULTIMA_SYNC = "ultima_sync_ms"
        private const val K_DEVICE_ID = "device_id"
        private val CABECERA = listOf(
            "id", "capturado_en", "nombre", "apellidos", "colegio", "telefono",
            "ano_curso", "inscrito_otro_preu", "staff_email", "device_id",
        )

        @Volatile private var instancia: CapturaStore? = null

        fun get(context: Context): CapturaStore =
            instancia ?: synchronized(this) {
                instancia ?: CapturaStore(context).also { instancia = it }
            }

        private fun csv(valor: String): String =
            if (valor.any { it == ',' || it == '"' || it == '\n' }) "\"" + valor.replace("\"", "\"\"") + "\""
            else valor

        private fun parsearCsv(linea: String): List<String> {
            val campos = mutableListOf<String>()
            val actual = StringBuilder()
            var entreComillas = false
            var i = 0
            while (i < linea.length) {
                val ch = linea[i]
                when {
                    entreComillas && ch == '"' && i + 1 < linea.length && linea[i + 1] == '"' -> { actual.append('"'); i++ }
                    ch == '"' -> entreComillas = !entreComillas
                    ch == ',' && !entreComillas -> { campos.add(actual.toString()); actual.clear() }
                    else -> actual.append(ch)
                }
                i++
            }
            campos.add(actual.toString())
            return campos
        }
    }
}
