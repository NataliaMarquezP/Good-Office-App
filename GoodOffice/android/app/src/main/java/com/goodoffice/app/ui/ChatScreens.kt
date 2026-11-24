package com.goodoffice.app.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.goodoffice.app.data.Documento
import com.goodoffice.app.data.Mensaje
import com.goodoffice.app.data.NuevoMensaje
import com.goodoffice.app.data.Sesion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private val coloresAvatar = listOf(Colores.Azul, Colores.Navy, Colores.Cielo, Colores.Ok, Colores.Adv)

@Composable
fun ChatsScreen(nav: NavHostController) {
    val convs = cargar { Sesion.api.conversaciones() }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Titulo("Mensajes")
        ErrorCarga(convs.error, convs.recargar)
        LazyColumn {
            items(convs.datos.orEmpty()) { c ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { nav.navigate("chat/${c.id}/${Uri.encode(c.nombre)}") }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(c.nombre, coloresAvatar[c.id % coloresAvatar.size])
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.nombre, fontWeight = FontWeight.SemiBold)
                        Text(c.ultimoMensaje, color = Colores.Sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(hora(c.ultimaFecha), color = Colores.Sub, fontSize = 11.sp)
                }
                HorizontalDivider(color = Colores.Linea)
            }
        }
    }
}

@Composable
fun ChatScreen(nav: NavHostController, conversacionId: Int, nombre: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val mensajes = remember { mutableStateListOf<Mensaje>() }
    var texto by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val lista = rememberLazyListState()

    // Consulta periódica (polling) cada 3 segundos pidiendo solo los mensajes nuevos.
    LaunchedEffect(conversacionId) {
        while (true) {
            try {
                val desde = mensajes.lastOrNull()?.enviadoEn
                val nuevos = Sesion.api.mensajes(conversacionId, desde)
                val ids = mensajes.map { it.id }.toSet()
                mensajes.addAll(nuevos.filter { it.id !in ids })
                error = null
            } catch (e: Exception) {
                error = mensajeError(e)
            }
            delay(3000)
        }
    }
    LaunchedEffect(mensajes.size) { if (mensajes.isNotEmpty()) lista.animateScrollToItem(mensajes.size - 1) }

    // Adjuntar documento: se sube primero a /api/documentos y luego se envía el mensaje con su id.
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val doc = subirArchivo(ctx, uri, conversacionId)
                Sesion.api.enviarMensaje(conversacionId, NuevoMensaje("Compartió un documento", doc.id))
            } catch (e: Exception) {
                error = mensajeError(e)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.padding(horizontal = 8.dp)) { Encabezado(nombre, nav) }
        error?.let { Text(it, color = Colores.Mal, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp)) }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 14.dp),
            state = lista,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mensajes, key = { it.id }) { m -> Burbuja(m, nav) }
        }
        Row(
            Modifier.fillMaxWidth().background(Colores.Superficie).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { selector.launch("*/*") }) {
                Icon(Icons.Filled.Add, contentDescription = "Adjuntar documento", tint = Colores.Azul)
            }
            OutlinedTextField(
                value = texto, onValueChange = { texto = it }, placeholder = { Text("Escribe un mensaje") },
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), maxLines = 4
            )
            IconButton(onClick = {
                val t = texto.trim()
                if (t.isEmpty()) return@IconButton
                texto = ""
                scope.launch {
                    try {
                        Sesion.api.enviarMensaje(conversacionId, NuevoMensaje(t))
                    } catch (e: Exception) {
                        error = mensajeError(e)
                        texto = t
                    }
                }
            }) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Colores.Azul) }
        }
    }
}

@Composable
private fun Burbuja(m: Mensaje, nav: NavHostController) {
    val fondo = if (m.esMio) Colores.Azul else Colores.Superficie
    val tinta = if (m.esMio) Color.White else Colores.Tinta
    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.esMio) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = 280.dp)
                .background(fondo, RoundedCornerShape(16.dp))
                .border(1.dp, if (m.esMio) fondo else Colores.Linea, RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            if (!m.esMio) Text(m.autor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Colores.Azul)
            Text(m.texto, color = tinta, fontSize = 14.sp)
            m.documento?.let { d -> AdjuntoChip(d, m.esMio) { nav.navigate("documentos") } }
            Text(hora(m.enviadoEn), color = tinta.copy(alpha = .7f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun AdjuntoChip(d: Documento, esMio: Boolean, alPulsar: () -> Unit) {
    Row(
        Modifier.padding(vertical = 6.dp)
            .background(if (esMio) Color.White.copy(alpha = .16f) else Colores.Hielo, RoundedCornerShape(10.dp))
            .clickable(onClick = alPulsar)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(extension(d.nombre), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = if (esMio) Color.White else Colores.Azul)
        Spacer(Modifier.size(8.dp))
        Column {
            Text(d.nombre, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (esMio) Color.White else Colores.Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tamano(d.tamanoBytes), fontSize = 10.sp, color = if (esMio) Color.White else Colores.Sub)
        }
    }
}

fun extension(nombre: String) = nombre.substringAfterLast('.', "DOC").uppercase().take(4)

fun tamano(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

/** Lee el archivo elegido y lo sube como multipart/form-data. */
suspend fun subirArchivo(ctx: android.content.Context, uri: Uri, conversacionId: Int?): Documento {
    val cr = ctx.contentResolver
    var nombre = "archivo"
    cr.query(uri, null, null, null, null)?.use { c ->
        val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (i >= 0 && c.moveToFirst()) nombre = c.getString(i)
    }
    val tipo = cr.getType(uri) ?: "application/octet-stream"
    val bytes = cr.openInputStream(uri)!!.use { it.readBytes() }
    val parte = MultipartBody.Part.createFormData("archivo", nombre, bytes.toRequestBody(tipo.toMediaTypeOrNull()))
    val conv = conversacionId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
    return Sesion.api.subirDocumento(parte, conv)
}
