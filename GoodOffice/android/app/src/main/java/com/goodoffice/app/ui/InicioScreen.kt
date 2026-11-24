package com.goodoffice.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.goodoffice.app.data.Sesion

@Composable
fun InicioScreen(nav: NavHostController) {
    val ambiente = cargar { Sesion.api.ambiente() }
    val reuniones = cargar { Sesion.api.reuniones() }
    val alertas = cargar { Sesion.api.alertas() }
    val nombre = Sesion.usuario?.nombre ?: ""

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hola,", color = Colores.Sub, fontSize = 14.sp)
                Text(nombre, color = Colores.Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = {
                Sesion.cerrar()
                nav.navigate("login") { popUpTo(0) }
            }) { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión", tint = Colores.Sub) }
        }

        // Tarjeta principal con el ambiente de la primera sala
        val sala = ambiente.datos?.firstOrNull()
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Colores.Navy, Colores.Azul, Colores.Cielo)), RoundedCornerShape(20.dp))
                .clickable { nav.navigate("ambiente") }
                .padding(16.dp)
        ) {
            Etiqueta("Ambiente ahora", Color.White.copy(alpha = .75f))
            Text(sala?.sala ?: "Cargando…", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sala?.sensores?.forEach { s ->
                    Column(
                        Modifier.weight(1f).background(Color.White.copy(alpha = .12f), RoundedCornerShape(12.dp)).padding(9.dp)
                    ) {
                        Text("${"%.1f".format(s.valor)} ${s.unidad}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(nombreTipo(s.tipo), color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
                    }
                }
            }
        }
        ErrorCarga(ambiente.error, ambiente.recargar)

        // Accesos rápidos
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Acceso("Chats", Icons.Filled.Email) { nav.navigate("chats") }
            Acceso("Documentos", Icons.AutoMirrored.Filled.List) { nav.navigate("documentos") }
            Acceso("Agendar", Icons.Filled.Add) { nav.navigate("agendar") }
            Acceso("Alertas", Icons.Filled.Warning) { nav.navigate("alertas") }
        }

        // Alertas activas
        val activas = alertas.datos.orEmpty()
        if (activas.isNotEmpty()) {
            Tarjeta(Modifier.clickable { nav.navigate("alertas") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Colores.Adv)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${activas.size} alerta(s) de ambiente activas", fontWeight = FontWeight.SemiBold)
                        Text(activas.first().mensaje, color = Colores.Sub, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Próximas reuniones
        Etiqueta("Próximas reuniones")
        reuniones.datos?.take(3)?.forEach { r ->
            Tarjeta(Modifier.clickable { nav.navigate("reuniones") }) {
                Text(r.titulo, fontWeight = FontWeight.SemiBold)
                Text("${fechaLarga(r.inicio)} · ${r.sala}", color = Colores.Sub, fontSize = 12.sp)
            }
        }
        if (reuniones.datos?.isEmpty() == true) Text("No hay reuniones agendadas.", color = Colores.Sub)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Acceso(texto: String, icono: ImageVector, alPulsar: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = alPulsar)) {
        Box(
            Modifier.size(52.dp)
                .background(Colores.Superficie, RoundedCornerShape(14.dp))
                .border(1.dp, Colores.Linea, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(icono, contentDescription = texto, tint = Colores.Azul) }
        Text(texto, fontSize = 11.sp, color = Colores.Sub, modifier = Modifier.padding(top = 6.dp))
    }
}
