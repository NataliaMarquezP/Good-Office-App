package com.goodoffice.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Paleta azul del prototipo
object Colores {
    val Fondo = Color(0xFFF3F7FD)
    val Superficie = Color(0xFFFFFFFF)
    val Tinta = Color(0xFF0F1F3A)
    val Sub = Color(0xFF5D6F8C)
    val Linea = Color(0xFFE1E9F5)
    val Navy = Color(0xFF0B2A5B)
    val Azul = Color(0xFF1F5FD1)
    val Cielo = Color(0xFF4EA3F0)
    val Hielo = Color(0xFFE4EFFF)
    val Ok = Color(0xFF1F9D6B)
    val OkFondo = Color(0xFFE3F6EE)
    val Adv = Color(0xFFC98A06)
    val AdvFondo = Color(0xFFFDF3DC)
    val Mal = Color(0xFFD2453B)
    val MalFondo = Color(0xFFFDE7E5)
}

private val esquema = lightColorScheme(
    primary = Colores.Azul,
    onPrimary = Color.White,
    secondary = Colores.Cielo,
    background = Colores.Fondo,
    surface = Colores.Superficie,
    onBackground = Colores.Tinta,
    onSurface = Colores.Tinta,
    surfaceVariant = Colores.Hielo,
    outline = Colores.Linea,
    error = Colores.Mal
)

@Composable
fun GoodOfficeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, content = content)
}

/** Tarjeta blanca con borde suave, como en el prototipo. */
@Composable
fun Tarjeta(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Colores.Superficie, RoundedCornerShape(16.dp))
            .border(1.dp, Colores.Linea, RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
fun Etiqueta(texto: String, color: Color = Colores.Sub) {
    Text(texto.uppercase(), fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = color)
}

/** Píldora de estado: ok / advertencia / critica. */
@Composable
fun Estado(estado: String) {
    val (texto, color, fondo) = when (estado) {
        "critica" -> Triple("Crítico", Colores.Mal, Colores.MalFondo)
        "advertencia" -> Triple("Atención", Colores.Adv, Colores.AdvFondo)
        else -> Triple("Óptimo", Colores.Ok, Colores.OkFondo)
    }
    Box(Modifier.background(fondo, RoundedCornerShape(99.dp)).padding(horizontal = 9.dp, vertical = 3.dp)) {
        Text(texto, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Avatar(nombre: String, color: Color = Colores.Azul) {
    val iniciales = nombre.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(40.dp).background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(iniciales.ifEmpty { "?" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun Titulo(texto: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = 8.dp)) {
        Text(texto, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Colores.Navy)
    }
}

// ---------- Fechas ----------
private val es = Locale("es", "CL")

fun fecha(iso: String?): LocalDateTime? = try {
    iso?.let { LocalDateTime.parse(it.take(19)) }
} catch (e: Exception) {
    null
}

fun hora(iso: String?): String = fecha(iso)?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: ""
fun diaCorto(iso: String?): String = fecha(iso)?.format(DateTimeFormatter.ofPattern("EEE d", es)) ?: ""
fun fechaLarga(iso: String?): String = fecha(iso)?.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM, HH:mm", es)) ?: ""

fun nombreTipo(tipo: String) = when (tipo) {
    "temperatura" -> "Temperatura"
    "humedad" -> "Humedad"
    "luminosidad" -> "Luminosidad"
    else -> tipo
}
