package com.goodoffice.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodoffice.app.data.EstadoSensor
import com.goodoffice.app.data.NuevaLectura
import com.goodoffice.app.data.Sesion
import kotlinx.coroutines.delay

/**
 * Lee el sensor de luz real del teléfono (Sensor.TYPE_LIGHT, en lux).
 * Devuelve null si el teléfono (o el emulador) no lo tiene.
 */
@Composable
fun rememberSensorLuz(): Float? {
    val ctx = LocalContext.current
    var lux by remember { mutableStateOf<Float?>(null) }
    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                lux = e.values[0]
            }

            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sm.unregisterListener(listener) }
    }
    return lux
}

@Composable
fun AmbienteScreen() {
    val salas = cargar { Sesion.api.salas() }
    var sala by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(salas.datos) { if (sala == null) sala = salas.datos?.firstOrNull() }

    // Se actualiza cada 15 segundos.
    var tic by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            tic++
        }
    }
    val ambiente = cargar(sala, tic) { sala?.let { Sesion.api.ambiente(it).firstOrNull() } }

    val lux = rememberSensorLuz()
    val luxActual by rememberUpdatedState(lux)
    var enviar by remember { mutableStateOf(true) }
    var ultimoEnvio by remember { mutableStateOf("") }

    // Envío periódico de la luz medida por el teléfono a la API.
    LaunchedEffect(enviar, sala) {
        while (enviar && sala != null) {
            val valor = luxActual
            if (valor != null) {
                try {
                    val l = Sesion.api.enviarLectura(NuevaLectura(sala!!, "luminosidad", valor.toDouble()))
                    ultimoEnvio = "Enviado ${hora(l.fechaHora)}"
                } catch (e: Exception) {
                    ultimoEnvio = mensajeError(e)
                }
            }
            delay(10_000)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Titulo("Ambiente")
        ErrorCarga(salas.error, salas.recargar)

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            salas.datos.orEmpty().forEach { s ->
                val on = s == sala
                Text(
                    s, fontSize = 13.sp, color = if (on) Color.White else Colores.Sub,
                    modifier = Modifier
                        .background(if (on) Colores.Navy else Colores.Superficie, RoundedCornerShape(10.dp))
                        .border(1.dp, Colores.Linea, RoundedCornerShape(10.dp))
                        .clickable { sala = s }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Sensor de luz del teléfono
        Tarjeta {
            Etiqueta("Sensor de luz del teléfono")
            if (lux == null) {
                Text("Este dispositivo no tiene sensor de luz (en el emulador se puede simular en Extended controls → Virtual sensors).", color = Colores.Sub, fontSize = 13.sp)
            } else {
                Text("${"%.0f".format(lux)} lx", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Colores.Navy)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Enviar a la API cada 10 s", fontSize = 14.sp)
                    Text(if (enviar) "Sala: ${sala ?: "-"} · $ultimoEnvio" else "Envío pausado", color = Colores.Sub, fontSize = 11.sp)
                }
                Switch(checked = enviar, onCheckedChange = { enviar = it })
            }
        }

        ErrorCarga(ambiente.error, ambiente.recargar)
        ambiente.datos?.sensores?.forEach { s -> TarjetaSensor(sala!!, s, tic) }
        Text(
            "Temperatura y humedad vienen de sensores simulados en el servidor. La luminosidad viene del teléfono cuando está enviando.",
            color = Colores.Sub, fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TarjetaSensor(sala: String, s: EstadoSensor, tic: Int) {
    val historial = cargar(sala, s.tipo, tic) { Sesion.api.historial(sala, s.tipo, 24) }
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Etiqueta(nombreTipo(s.tipo))
                Text("${"%.1f".format(s.valor)} ${s.unidad}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Colores.Tinta)
            }
            Estado(s.estado)
        }
        BarraRango(s)
        Text(
            "Ideal ${"%.0f".format(s.minimo)}–${"%.0f".format(s.maximo)} ${s.unidad} · origen: ${s.origen} · ${hora(s.fechaHora)}",
            color = Colores.Sub, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)
        )
        GraficoLinea(historial.datos.orEmpty().map { it.valor }, s.minimo, s.maximo)
        Text("Últimas 24 horas", color = Colores.Sub, fontSize = 10.sp)
    }
}

/** Barra con la franja ideal en verde y una marca con el valor actual. */
@Composable
private fun BarraRango(s: EstadoSensor) {
    val ancho = s.maximo - s.minimo
    val min = s.minimo - ancho * .6
    val max = s.maximo + ancho * .6
    fun pos(v: Double) = ((v.coerceIn(min, max) - min) / (max - min)).toFloat()
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 10.dp).height(16.dp)) {
        val w = maxWidth
        Box(Modifier.fillMaxWidth().height(8.dp).offset(y = 4.dp).background(Colores.Linea, RoundedCornerShape(4.dp)))
        Box(
            Modifier.offset(x = w * pos(s.minimo), y = 4.dp).width(w * (pos(s.maximo) - pos(s.minimo))).height(8.dp)
                .background(Colores.OkFondo, RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFFB8E6D2), RoundedCornerShape(4.dp))
        )
        Box(Modifier.offset(x = w * pos(s.valor) - 2.dp).width(4.dp).fillMaxHeight().background(Colores.Navy, RoundedCornerShape(2.dp)))
    }
}
