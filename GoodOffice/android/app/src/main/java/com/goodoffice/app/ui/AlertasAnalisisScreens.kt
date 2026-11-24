package com.goodoffice.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.goodoffice.app.data.Alerta
import com.goodoffice.app.data.Sesion
import kotlinx.coroutines.launch

@Composable
fun AlertasScreen(nav: NavHostController) {
    var todas by remember { mutableStateOf(false) }
    val alertas = cargar(todas) { Sesion.api.alertas(todas) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Encabezado("Alertas", nav)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar también las resueltas", Modifier.weight(1f), color = Colores.Sub, fontSize = 13.sp)
            Switch(checked = todas, onCheckedChange = { todas = it })
        }
        ErrorCarga(alertas.error, alertas.recargar)
        if (alertas.datos?.isEmpty() == true) {
            Tarjeta { Text("Sin alertas activas. Todo el ambiente está dentro del rango ideal.", color = Colores.Ok) }
        }
        alertas.datos.orEmpty().forEach { a ->
            TarjetaAlerta(a) {
                scope.launch {
                    try {
                        Sesion.api.resolverAlerta(a.id)
                        alertas.recargar()
                    } catch (_: Exception) {
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TarjetaAlerta(a: Alerta, alResolver: () -> Unit) {
    val color = when {
        a.resuelta -> Colores.Ok
        a.nivel == "critica" -> Colores.Mal
        else -> Colores.Adv
    }
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(16.dp)).background(Colores.Superficie)
    ) {
        Box(Modifier.width(5.dp).fillMaxHeight().background(color))
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${nombreTipo(a.tipo)} · ${a.sala}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Estado(if (a.resuelta) "ok" else a.nivel)
            }
            Text(a.mensaje, color = Colores.Sub, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
            Text(
                "Sugerencia: ${a.sugerencia}", color = Colores.Navy, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().background(Colores.Hielo, RoundedCornerShape(10.dp)).padding(8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${diaCorto(a.fechaHora)} ${hora(a.fechaHora)}", color = Colores.Sub, fontSize = 11.sp, modifier = Modifier.weight(1f))
                if (!a.resuelta) TextButton(onClick = alResolver) { Text("Marcar atendida") }
            }
        }
    }
}

@Composable
fun AnalisisScreen() {
    val analisis = cargar { Sesion.api.analisis(7) }
    val a = analisis.datos

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Titulo("Análisis · últimos 7 días")
        ErrorCarga(analisis.error, analisis.recargar)
        if (a == null) return@Column

        // KPIs en grilla de 2 columnas
        a.kpis.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fila.forEach { k ->
                    Tarjeta(Modifier.weight(1f)) {
                        Etiqueta(k.nombre)
                        Text(k.valor, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Colores.Navy)
                        Text(k.detalle, color = Colores.Sub, fontSize = 11.sp)
                    }
                }
                if (fila.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Tarjeta {
            Etiqueta("Mensajes por día")
            GraficoBarras(a.mensajesPorDia.map { it.valor }, a.mensajesPorDia.map { diaCorto(it.fechaHora).take(3) })
        }

        Tarjeta {
            Etiqueta("Temperatura promedio por día (°C)")
            GraficoLinea(a.temperaturaPromedioPorDia.map { it.valor }, 20.0, 24.0)
        }

        Tarjeta {
            Etiqueta("Confort por sala")
            a.confortPorSala.forEach { s ->
                val pct = s.valor.removeSuffix("%").toFloatOrNull() ?: 0f
                Column(Modifier.padding(top = 8.dp)) {
                    Row {
                        Text(s.nombre, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(s.valor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Colores.Linea)) {
                        Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().background(if (pct < 70) Colores.Adv else Colores.Azul))
                    }
                    Text(s.detalle, color = Colores.Sub, fontSize = 11.sp)
                }
            }
        }

        Tarjeta {
            Etiqueta("Recomendaciones de mejora")
            a.recomendaciones.forEach { r ->
                Text("• $r", fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
