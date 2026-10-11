package com.pre.capturadatos

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.view.WindowManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Modo Stand = fijar pantalla (screen pinning) + pantalla siempre encendida.
 *
 * Los celulares tambien se usan en la oficina, asi que no se pueden configurar como
 * "Device Owner" (eso exige restablecerlos de fabrica). Con la fijacion normal Android
 * permite salir con un gesto: para que pida el PIN del celular hay que activar en Ajustes
 * > Seguridad > Fijar aplicaciones > "Pedir PIN para dejar de fijar".
 */
object ModoStand {

    private const val PREFS = "modo_stand"
    private const val K_PIN = "pin_staff"

    fun activo(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    fun activar(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity.startLockTask()
    }

    fun desactivar(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        runCatching { activity.stopLockTask() }
    }

    fun pin(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(K_PIN, null)

    fun guardarPin(context: Context, pin: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(K_PIN, pin).apply()
    }
}

fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun generarQr(texto: String, lado: Int = 720): ImageBitmap {
    val matriz = QRCodeWriter().encode(
        texto, BarcodeFormat.QR_CODE, lado, lado,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1),
    )
    val pixeles = IntArray(lado * lado) { i ->
        if (matriz.get(i % lado, i / lado)) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixeles, lado, lado, Bitmap.Config.ARGB_8888).asImageBitmap()
}
