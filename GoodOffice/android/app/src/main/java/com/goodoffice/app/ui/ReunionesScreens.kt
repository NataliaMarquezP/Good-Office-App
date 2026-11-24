package com.goodoffice.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.goodoffice.app.data.NuevaReunion
import com.goodoffice.app.data.Sesion
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ReunionesScreen(nav: NavHostController) {
    val reuniones = cargar { Sesion.api.reuniones() }
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Titulo("Reuniones")
            ErrorCarga(reuniones.error, reuniones.recargar)
            val porDia = reuniones.datos.orEmpty().groupBy { fecha(it.inicio)?.toLocalDate() }
            porDia.forEach { (dia, lista) ->
                Etiqueta(dia?.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", java.util.Locale("es", "CL"))) ?: "")
                lista.forEach { r ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(hora(r.inicio), color = Colores.Sub, fontSize = 12.sp, modifier = Modifier.width(48.dp).padding(top = 10.dp))
                        Column(
                            Modifier.weight(1f)
                                .background(Colores.Azul, RoundedCornerShape(12.dp))
                                .padding(start = 4.dp)
                                .background(Colores.Hielo, RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                                .padding(10.dp)
                        ) {
                            Text(r.titulo, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${r.duracionMinutos} min · ${r.sala}", color = Colores.Sub, fontSize = 12.sp)
                            val n = r.invitados.split(",").count { it.isNotBlank() }
                            Text(
                                "$n invitado(s)" + if (r.correoEnviado) " · invitación enviada por correo" else "",
                                color = Colores.Azul, fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            if (reuniones.datos?.isEmpty() == true) Text("No hay reuniones próximas.", color = Colores.Sub)
            Spacer(Modifier.height(80.dp))
        }
        FloatingActionButton(
            onClick = { nav.navigate("agendar") },
            containerColor = Colores.Azul, contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "Agendar reunión") }
    }
}

@Composable
fun AgendarScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val usuarios = cargar { Sesion.api.usuarios() }
    val salas = cargar { Sesion.api.salas() }

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var horaInicio by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var duracion by remember { mutableStateOf(30) }
    var sala by remember { mutableStateOf("") }
    val invitados = remember { mutableStateListOf<String>() }
    var enviarCorreo by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Encabezado("Agendar reunión", nav)
        OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(descripcion, { descripcion = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Caja("Fecha", dia.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), Modifier.weight(1f)) {
                DatePickerDialog(ctx, { _, a, m, d -> dia = LocalDate.of(a, m + 1, d) }, dia.year, dia.monthValue - 1, dia.dayOfMonth).show()
            }
            Caja("Hora", horaInicio.format(DateTimeFormatter.ofPattern("HH:mm")), Modifier.weight(1f)) {
                TimePickerDialog(ctx, { _, h, m -> horaInicio = LocalTime.of(h, m) }, horaInicio.hour, horaInicio.minute, true).show()
            }
        }

        Etiqueta("Duración")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(15, 30, 45, 60, 90).forEach { m -> Opcion("$m min", duracion == m) { duracion = m } }
        }

        Etiqueta("Sala")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            salas.datos.orEmpty().forEach { s -> Opcion(s, sala == s) { sala = s } }
        }

        Etiqueta("Invitados")
        Tarjeta {
            usuarios.datos.orEmpty().filter { it.id != Sesion.usuario?.id }.forEach { u ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                    if (u.correo in invitados) invitados.remove(u.correo) else invitados.add(u.correo)
                }) {
                    Checkbox(checked = u.correo in invitados, onCheckedChange = {
                        if (it) invitados.add(u.correo) else invitados.remove(u.correo)
                    })
                    Column {
                        Text(u.nombre, fontSize = 14.sp)
                        Text(u.correo, color = Colores.Sub, fontSize = 11.sp)
                    }
                }
            }
        }

        Tarjeta {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Enviar invitación por correo", fontWeight = FontWeight.SemiBold)
                    Text("Llega al correo corporativo con archivo de calendario (.ics)", color = Colores.Sub, fontSize = 12.sp)
                }
                Switch(checked = enviarCorreo, onCheckedChange = { enviarCorreo = it })
            }
        }

        error?.let { Text(it, color = Colores.Mal) }
        Button(
            onClick = {
                if (titulo.isBlank() || sala.isBlank()) {
                    error = "Completa el título y elige una sala"
                    return@Button
                }
                guardando = true
                error = null
                scope.launch {
                    try {
                        val inicio = LocalDateTime.of(dia, horaInicio).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        Sesion.api.crearReunion(NuevaReunion(titulo, descripcion, inicio, duracion, sala, invitados.toList(), enviarCorreo))
                        Toast.makeText(ctx, "Reunión agendada", Toast.LENGTH_SHORT).show()
                        nav.popBackStack()
                    } catch (e: retrofit2.HttpException) {
                        error = if (e.code() == 409) "La sala ya está ocupada en ese horario" else mensajeError(e)
                    } catch (e: Exception) {
                        error = mensajeError(e)
                    } finally {
                        guardando = false
                    }
                }
            },
            enabled = !guardando,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(if (guardando) "Guardando…" else "Agendar y enviar invitación") }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Caja(etiqueta: String, valor: String, modifier: Modifier, alPulsar: () -> Unit) {
    Column(modifier) {
        Etiqueta(etiqueta)
        Text(
            valor,
            Modifier.fillMaxWidth().padding(top = 5.dp)
                .background(Colores.Superficie, RoundedCornerShape(12.dp))
                .border(1.dp, Colores.Linea, RoundedCornerShape(12.dp))
                .clickable(onClick = alPulsar)
                .padding(12.dp)
        )
    }
}

@Composable
private fun Opcion(texto: String, elegido: Boolean, alPulsar: () -> Unit) {
    Text(
        texto,
        color = if (elegido) Color.White else Colores.Tinta,
        fontSize = 13.sp,
        modifier = Modifier
            .background(if (elegido) Colores.Navy else Colores.Superficie, RoundedCornerShape(99.dp))
            .border(1.dp, if (elegido) Colores.Navy else Colores.Linea, RoundedCornerShape(99.dp))
            .clickable(onClick = alPulsar)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}
