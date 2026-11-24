package com.goodoffice.app.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ---------- Modelos (mismos nombres que el JSON de la API) ----------

data class LoginRequest(val correo: String, val clave: String)
data class Usuario(val id: Int, val nombre: String, val correo: String, val cargo: String, val area: String)
data class LoginResponse(val token: String, val usuario: Usuario)

data class Conversacion(
    val id: Int, val nombre: String, val esGrupo: Boolean,
    val ultimoMensaje: String, val ultimaFecha: String?
)

data class Documento(
    val id: Int, val nombre: String, val tipoMime: String, val tamanoBytes: Long,
    val subidoPorId: Int, val conversacionId: Int?, val subidoEn: String
)

data class Mensaje(
    val id: Int, val conversacionId: Int, val usuarioId: Int, val autor: String,
    val texto: String, val enviadoEn: String, val esMio: Boolean, val documento: Documento?
)

data class NuevoMensaje(val texto: String, val documentoId: Int? = null)

data class Reunion(
    val id: Int, val titulo: String, val descripcion: String, val inicio: String,
    val duracionMinutos: Int, val sala: String, val organizadorId: Int,
    val invitados: String, val correoEnviado: Boolean
)

data class NuevaReunion(
    val titulo: String, val descripcion: String, val inicio: String, val duracionMinutos: Int,
    val sala: String, val invitados: List<String>, val enviarCorreo: Boolean = true
)

data class EstadoSensor(
    val tipo: String, val valor: Double, val unidad: String, val minimo: Double, val maximo: Double,
    val estado: String, val origen: String, val fechaHora: String
)

data class AmbienteSala(val sala: String, val sensores: List<EstadoSensor>)
data class PuntoSerie(val fechaHora: String, val valor: Double)
data class NuevaLectura(val sala: String, val tipo: String, val valor: Double, val origen: String = "telefono")
data class Lectura(val id: Int, val sala: String, val tipo: String, val valor: Double, val unidad: String, val fechaHora: String)

data class Alerta(
    val id: Int, val sala: String, val tipo: String, val nivel: String, val valor: Double,
    val mensaje: String, val sugerencia: String, val fechaHora: String, val resuelta: Boolean
)

data class Kpi(val nombre: String, val valor: String, val detalle: String)

data class Analisis(
    val kpis: List<Kpi>,
    val mensajesPorDia: List<PuntoSerie>,
    val temperaturaPromedioPorDia: List<PuntoSerie>,
    val confortPorSala: List<Kpi>,
    val recomendaciones: List<String>
)

// ---------- Endpoints REST ----------

interface GoodOfficeApi {
    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @GET("api/usuarios")
    suspend fun usuarios(): List<Usuario>

    @GET("api/conversaciones")
    suspend fun conversaciones(): List<Conversacion>

    @GET("api/conversaciones/{id}/mensajes")
    suspend fun mensajes(@Path("id") id: Int, @Query("desde") desde: String? = null): List<Mensaje>

    @POST("api/conversaciones/{id}/mensajes")
    suspend fun enviarMensaje(@Path("id") id: Int, @Body m: NuevoMensaje)

    @GET("api/documentos")
    suspend fun documentos(): List<Documento>

    @Multipart
    @POST("api/documentos")
    suspend fun subirDocumento(@Part archivo: MultipartBody.Part, @Part("conversacionId") conversacionId: RequestBody?): Documento

    @GET("api/reuniones")
    suspend fun reuniones(): List<Reunion>

    @POST("api/reuniones")
    suspend fun crearReunion(@Body r: NuevaReunion): Reunion

    @GET("api/sensores/salas")
    suspend fun salas(): List<String>

    @GET("api/sensores")
    suspend fun ambiente(@Query("sala") sala: String? = null): List<AmbienteSala>

    @GET("api/sensores/historial")
    suspend fun historial(@Query("sala") sala: String, @Query("tipo") tipo: String, @Query("horas") horas: Int = 24): List<PuntoSerie>

    @POST("api/sensores/lecturas")
    suspend fun enviarLectura(@Body l: NuevaLectura): Lectura

    @GET("api/alertas")
    suspend fun alertas(@Query("todas") todas: Boolean = false): List<Alerta>

    @PUT("api/alertas/{id}/resolver")
    suspend fun resolverAlerta(@Path("id") id: Int): Alerta

    @GET("api/analisis")
    suspend fun analisis(@Query("dias") dias: Int = 7): Analisis
}

/** Mantiene la URL del servidor, el token y el usuario de la sesión actual. */
object Sesion {
    var servidor: String = "http://10.0.2.2:5080/"
        private set
    var token: String? = null
    var usuario: Usuario? = null

    lateinit var api: GoodOfficeApi
        private set

    fun configurar(url: String) {
        servidor = if (url.endsWith("/")) url else "$url/"
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                token?.let { req.header("Authorization", "Bearer $it") }
                chain.proceed(req.build())
            }
            .addInterceptor(log)
            .build()
        api = Retrofit.Builder()
            .baseUrl(servidor)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoodOfficeApi::class.java)
    }

    fun cerrar() {
        token = null
        usuario = null
    }
}
