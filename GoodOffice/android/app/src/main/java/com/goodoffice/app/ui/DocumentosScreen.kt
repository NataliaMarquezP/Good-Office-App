package com.goodoffice.app.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.goodoffice.app.data.Documento
import com.goodoffice.app.data.Sesion
import kotlinx.coroutines.launch

@Composable
fun DocumentosScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val docs = cargar { Sesion.api.documentos() }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                subirArchivo(ctx, uri, null)
                Toast.makeText(ctx, "Documento subido", Toast.LENGTH_SHORT).show()
                docs.recargar()
            } catch (e: Exception) {
                Toast.makeText(ctx, mensajeError(e), Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Encabezado("Documentos", nav) {
            IconButton(onClick = { selector.launch("*/*") }) {
                Icon(Icons.Filled.Add, contentDescription = "Subir documento", tint = Colores.Azul)
            }
        }
        ErrorCarga(docs.error, docs.recargar)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(docs.datos.orEmpty()) { d ->
                Tarjeta(Modifier.clickable { descargar(ctx, d) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).background(colorTipo(d.nombre), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text(extension(d.nombre), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp) }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.nombre, fontWeight = FontWeight.SemiBold)
                            Text("${tamano(d.tamanoBytes)} · ${diaCorto(d.subidoEn)}", color = Colores.Sub, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun colorTipo(nombre: String) = when (extension(nombre)) {
    "PDF" -> Colores.Mal
    "XLSX", "XLS", "CSV" -> Colores.Ok
    "DOCX", "DOC" -> Colores.Azul
    else -> Colores.Navy
}

/** Descarga el archivo con el DownloadManager de Android, enviando el token. */
private fun descargar(ctx: Context, d: Documento) {
    val url = Uri.parse("${Sesion.servidor}api/documentos/${d.id}/descarga")
    val req = DownloadManager.Request(url)
        .setTitle(d.nombre)
        .addRequestHeader("Authorization", "Bearer ${Sesion.token}")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, d.nombre)
    (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
    Toast.makeText(ctx, "Descargando ${d.nombre}", Toast.LENGTH_SHORT).show()
}
