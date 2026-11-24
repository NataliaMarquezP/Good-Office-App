package com.goodoffice.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

/** Resultado de una llamada a la API para mostrar en pantalla. */
class Carga<T>(val datos: T?, val error: String?, val recargar: () -> Unit)

/** Llama a la API al abrir la pantalla (y cada vez que cambian las claves o se llama a recargar()). */
@Composable
fun <T> cargar(vararg claves: Any?, bloque: suspend () -> T): Carga<T> {
    var version by remember { mutableIntStateOf(0) }
    var datos by remember { mutableStateOf<T?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(version, *claves) {
        try {
            datos = bloque()
            error = null
        } catch (e: Exception) {
            error = mensajeError(e)
        }
    }
    return Carga(datos, error) { version++ }
}

fun mensajeError(e: Exception): String = when (e) {
    is retrofit2.HttpException -> when (e.code()) {
        401 -> "La sesión expiró. Vuelve a iniciar sesión."
        409 -> "Conflicto: " + (e.response()?.errorBody()?.string() ?: "")
        else -> "Error del servidor (${e.code()})"
    }
    is java.io.IOException -> "Sin conexión con la API"
    else -> e.message ?: "Error"
}

@Composable
fun ErrorCarga(error: String?, recargar: () -> Unit) {
    if (error == null) return
    Tarjeta {
        Text(error, color = Colores.Mal, fontSize = 14.sp)
        TextButton(onClick = recargar) { Text("Reintentar") }
    }
}

/** Encabezado con flecha para volver. */
@Composable
fun Encabezado(titulo: String, nav: NavHostController?, accion: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (nav != null) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Colores.Navy)
            }
        }
        Titulo(titulo, Modifier.weight(1f))
        accion()
    }
}

/** Gráfico de línea simple dibujado con Canvas (sin librerías externas). */
@Composable
fun GraficoLinea(valores: List<Double>, minIdeal: Double? = null, maxIdeal: Double? = null, color: Color = Colores.Azul) {
    if (valores.size < 2) {
        Text("Sin datos suficientes", color = Colores.Sub, fontSize = 12.sp)
        return
    }
    val min = minOf(valores.min(), minIdeal ?: valores.min())
    val max = maxOf(valores.max(), maxIdeal ?: valores.max())
    val rango = (max - min).takeIf { it > 0 } ?: 1.0
    Canvas(Modifier.fillMaxWidth().height(80.dp).padding(top = 8.dp)) {
        fun y(v: Double) = (size.height - ((v - min) / rango * size.height)).toFloat()
        if (minIdeal != null && maxIdeal != null) {
            drawRect(
                Colores.OkFondo, topLeft = Offset(0f, y(maxIdeal)),
                size = Size(size.width, y(minIdeal) - y(maxIdeal))
            )
        }
        val paso = size.width / (valores.size - 1)
        val path = Path()
        valores.forEachIndexed { i, v ->
            if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * paso, y(v))
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Gráfico de barras simple con etiqueta bajo cada barra. */
@Composable
fun GraficoBarras(valores: List<Double>, etiquetas: List<String>) {
    if (valores.isEmpty()) return
    val max = valores.max().takeIf { it > 0 } ?: 1.0
    Column {
        Canvas(Modifier.fillMaxWidth().height(110.dp).padding(top = 10.dp)) {
            val ancho = size.width / valores.size
            valores.forEachIndexed { i, v ->
                val alto = (v / max * size.height).toFloat()
                drawRoundRect(
                    if (v == valores.max()) Colores.Azul else Colores.Cielo,
                    topLeft = Offset(i * ancho + ancho * .15f, size.height - alto),
                    size = Size(ancho * .7f, alto),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            etiquetas.forEach { Text(it, fontSize = 10.sp, color = Colores.Sub) }
        }
    }
}
