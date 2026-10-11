package com.pre.capturadatos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class StandConfigTest {

    private fun fechaUtc(iso: String) = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(iso)!!

    @Test
    fun codigoUsaLaFechaDeEcuadorNoLaUtc() {
        // 16-oct 22:30 en Quito ya es 17-oct en UTC: el codigo debe seguir siendo del 16.
        assertEquals("STAND-20261016", StandConfig.codigoStand(fechaUtc("2026-10-17T03:30:00Z")))
        assertEquals("STAND-20261017", StandConfig.codigoStand(fechaUtc("2026-10-17T13:00:00Z")))
    }

    @Test
    fun enlaceAbreElChatDelPreuConElCodigo() {
        val enlace = StandConfig.enlaceWhatsApp()
        assertTrue(enlace.startsWith("https://wa.me/593995412463?text="))
        assertFalse("WhatsApp muestra '+' literal si el espacio no va como %20", enlace.contains("+"))
        val texto = URLDecoder.decode(enlace.substringAfter("text="), "UTF-8")
        assertTrue(texto.contains("(código ${StandConfig.codigoStand()})"))
    }
}
