package com.goodoffice.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goodoffice.app.data.LoginRequest
import com.goodoffice.app.data.Sesion
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(alEntrar: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("goodoffice", Context.MODE_PRIVATE) }
    var servidor by remember { mutableStateOf(prefs.getString("servidor", "http://10.0.2.2:5080/")!!) }
    var correo by remember { mutableStateOf(prefs.getString("correo", "demo@goodoffice.local")!!) }
    var clave by remember { mutableStateOf("goodoffice123") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Colores.Navy, Colores.Azul, Colores.Cielo)))
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Box(
            Modifier.size(64.dp).background(Color.White, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) { Text("GO", color = Colores.Azul, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }
        Spacer(Modifier.height(18.dp))
        Text("Good Office", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(
            "Comunicación interna y un ambiente de trabajo cómodo, en una sola app.",
            color = Color.White.copy(alpha = .85f), fontSize = 15.sp
        )
        Spacer(Modifier.height(32.dp))

        Column(
            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(22.dp)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = correo, onValueChange = { correo = it }, label = { Text("Correo corporativo") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            OutlinedTextField(
                value = clave, onValueChange = { clave = it }, label = { Text("Clave") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            OutlinedTextField(
                value = servidor, onValueChange = { servidor = it }, label = { Text("Servidor de la API") },
                supportingText = { Text("Emulador: http://10.0.2.2:5080/  ·  Teléfono: http://IP-del-PC:5080/") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            error?.let { Text(it, color = Colores.Mal, fontSize = 13.sp) }
            Button(
                onClick = {
                    cargando = true
                    error = null
                    scope.launch {
                        try {
                            Sesion.configurar(servidor.trim())
                            val r = Sesion.api.login(LoginRequest(correo.trim(), clave))
                            Sesion.token = r.token
                            Sesion.usuario = r.usuario
                            prefs.edit().putString("servidor", servidor.trim()).putString("correo", correo.trim()).apply()
                            alEntrar()
                        } catch (e: retrofit2.HttpException) {
                            error = if (e.code() == 401) "Correo o clave incorrectos" else "Error del servidor (${e.code()})"
                        } catch (e: Exception) {
                            error = "No se pudo conectar con $servidor. ¿Está corriendo la API?"
                        } finally {
                            cargando = false
                        }
                    }
                },
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (cargando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Iniciar sesión", fontWeight = FontWeight.SemiBold)
            }
            Text("Usuario de prueba: demo@goodoffice.local / goodoffice123", color = Colores.Sub, fontSize = 12.sp)
        }
    }
}
