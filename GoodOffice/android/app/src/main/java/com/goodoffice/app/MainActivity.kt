package com.goodoffice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.goodoffice.app.ui.AgendarScreen
import com.goodoffice.app.ui.AlertasScreen
import com.goodoffice.app.ui.AmbienteScreen
import com.goodoffice.app.ui.AnalisisScreen
import com.goodoffice.app.ui.ChatScreen
import com.goodoffice.app.ui.ChatsScreen
import com.goodoffice.app.ui.Colores
import com.goodoffice.app.ui.DocumentosScreen
import com.goodoffice.app.ui.GoodOfficeTheme
import com.goodoffice.app.ui.InicioScreen
import com.goodoffice.app.ui.LoginScreen
import com.goodoffice.app.ui.ReunionesScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoodOfficeTheme { App() }
        }
    }
}

private data class Pestana(val ruta: String, val texto: String, val icono: ImageVector)

private val pestanas = listOf(
    Pestana("inicio", "Inicio", Icons.Filled.Home),
    Pestana("chats", "Chats", Icons.Filled.Email),
    Pestana("reuniones", "Reuniones", Icons.Filled.DateRange),
    Pestana("ambiente", "Ambiente", Icons.Filled.Info),
    Pestana("analisis", "Análisis", Icons.Filled.Star),
)

@Composable
fun App() {
    val nav = rememberNavController()
    val actual by nav.currentBackStackEntryAsState()
    val ruta = actual?.destination?.route
    val conBarra = pestanas.any { it.ruta == ruta }

    Scaffold(
        containerColor = Colores.Fondo,
        bottomBar = { if (conBarra) BarraInferior(nav, ruta) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(nav, startDestination = "login") {
                composable("login") {
                    LoginScreen(alEntrar = {
                        nav.navigate("inicio") { popUpTo("login") { inclusive = true } }
                    })
                }
                composable("inicio") { InicioScreen(nav) }
                composable("chats") { ChatsScreen(nav) }
                composable(
                    "chat/{id}/{nombre}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType }, navArgument("nombre") { type = NavType.StringType })
                ) { e ->
                    ChatScreen(nav, e.arguments!!.getInt("id"), e.arguments!!.getString("nombre") ?: "")
                }
                composable("documentos") { DocumentosScreen(nav) }
                composable("reuniones") { ReunionesScreen(nav) }
                composable("agendar") { AgendarScreen(nav) }
                composable("ambiente") { AmbienteScreen() }
                composable("alertas") { AlertasScreen(nav) }
                composable("analisis") { AnalisisScreen() }
            }
        }
    }
}

@Composable
private fun BarraInferior(nav: NavHostController, ruta: String?) {
    NavigationBar(containerColor = Colores.Superficie) {
        pestanas.forEach { p ->
            NavigationBarItem(
                selected = ruta == p.ruta,
                onClick = {
                    nav.navigate(p.ruta) {
                        popUpTo("inicio") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(p.icono, contentDescription = p.texto) },
                label = { Text(p.texto) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Colores.Azul, selectedTextColor = Colores.Azul,
                    indicatorColor = Colores.Hielo, unselectedIconColor = Colores.Sub, unselectedTextColor = Colores.Sub
                )
            )
        }
    }
}
