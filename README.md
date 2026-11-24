# 🏢 Good Office

<p align="center">
  <strong>Aplicación móvil empresarial para la comunicación interna y el monitoreo inteligente del ambiente de trabajo.</strong>
</p>

<p align="center">
  Chats · Documentos · Reuniones · Sensores · Alertas · Análisis
</p>

---

## 📋 Descripción

**Good Office** centraliza las principales necesidades operativas de una organización en una sola aplicación móvil.

La plataforma permite:

- 💬 Comunicación interna entre equipos.
- 📁 Gestión de documentos compartidos.
- 📅 Organización de reuniones con invitaciones por correo corporativo.
- 🌡️ Monitoreo ambiental en tiempo real.
- 🚨 Generación automática de alertas.
- 🤖 Autorregulación de incidencias.
- 📊 Análisis de indicadores y recomendaciones de mejora.

A partir de las lecturas de temperatura, humedad y luminosidad, el sistema evalúa las condiciones de cada sala, detecta desviaciones respecto a los rangos ideales y genera recomendaciones para mantener un entorno de trabajo confortable y productivo.

> 🎓 Proyecto académico desarrollado para  **EPE3** del curso **Programación de aplicaciones móviles I**.

---

## 📚 Contenido

- [✨ Funcionalidades](#-funcionalidades)
- [🏗️ Arquitectura](#️-arquitectura)
- [🛠️ Tecnologías](#️-tecnologías)
- [📁 Estructura del repositorio](#-estructura-del-repositorio)
- [🚀 Cómo ejecutarlo](#-cómo-ejecutarlo)
- [🌐 Servicios web](#-servicios-web)
- [🌡️ Sensores, alertas y autorregulación](#️-sensores-alertas-y-autorregulación)
- [📧 Correo corporativo](#-correo-corporativo)
- [⚙️ Configuración](#️-configuración)

---

# ✨ Funcionalidades

| Módulo | Descripción |
|---------|------------|
| 🏠 **Inicio** | Resumen diario con condiciones ambientales, alertas activas y próximas reuniones. |
| 💬 **Chats** | Comunicación entre equipos mediante mensajes y archivos adjuntos. |
| 📁 **Documentos** | Gestión de archivos compartidos. |
| 📅 **Reuniones** | Agenda corporativa con invitaciones automáticas mediante correo electrónico e iCalendar (.ics). |
| 🌡️ **Ambiente** | Visualización de temperatura, humedad y luminosidad por sala con historial gráfico. Utiliza el sensor de luz real del dispositivo móvil. |
| 🚨 **Alertas** | Notificaciones automáticas cuando un valor sale del rango óptimo definido. |
| 📊 **Análisis** | Indicadores de confort ambiental, métricas históricas y recomendaciones de mejora continua. |


---

## 📖 Documentación de la API

<img src="docs/capturas/11-api.png" width="100%">

<details>
<summary><strong>Pruebas de endpoints REST (GET y POST)</strong></summary>

<br>

<img src="docs/capturas/13-api-prueba-get.png" width="100%">

<br>

<img src="docs/capturas/14-api-prueba-post.png" width="100%">

</details>

---

# 🏗️ Arquitectura

```text
┌──────────────────────────┐      HTTP / REST (JSON)      ┌───────────────────────────────┐
│      App Android         │ ───────────────────────────▶ │        API Good Office        │
│                          │                              │                               │
│ • Kotlin                │ ◀─────────────────────────── │ • ASP.NET Core (.NET 10)      │
│ • Jetpack Compose       │                              │ • REST JSON / XML             │
│ • Retrofit + Gson       │ POST /api/sensores/lecturas │ • SOAP + WSDL (SoapCore)      │
│ • Sensor de luz real    │                              │ • Gestión de alertas          │
└──────────────────────────┘                              │ • KPIs y análisis             │
                                                          │ • Correo corporativo (.ics)   │
┌──────────────────────────┐       SOAP (XML)            │ • Simulador de sensores       │
│       Cliente SOAP       │ ──────────────────────────▶ │                               │
│    (Postman / SoapUI)    │                              └──────────────┬────────────────┘
└──────────────────────────┘                                             │
                                                                          │ EF Core
                                                                  ┌───────▼───────┐
                                                                  │    SQLite     │
                                                                  │ goodoffice.db │
                                                                  └───────────────┘
```

---

# 🛠️ Tecnologías

| Capa | Tecnología |
|--------|------------|
| Backend | .NET 10, ASP.NET Core, Entity Framework Core |
| Base de datos | SQLite |
| Servicios web | REST (JSON/XML), SOAP (WSDL mediante SoapCore) |
| Documentación API | OpenAPI + Scalar |
| Correo | SMTP + iCalendar (.ics) |
| Android | Kotlin 2.0, Jetpack Compose, Material 3 |
| Networking móvil | Retrofit + Gson |
| Sensores | SensorManager |
| Build Android | Gradle 8.11, Android Gradle Plugin 8.7 |

---

# 📁 Estructura del repositorio

```text
GoodOffice/
│
├── server/
│   ├── GoodOffice.slnx
│   └── GoodOffice.Api/
│       ├── Controllers/     # Endpoints REST
│       ├── Soap/            # Servicio SOAP + WSDL
│       ├── Services/        # Sensores, correo y análisis
│       ├── Data/            # EF Core + SQLite
│       ├── Models/          # Entidades y DTOs
│       ├── wwwroot/
│       └── appsettings.json
│
├── android/
│   └── app/src/main/java/com/goodoffice/app/
│       ├── data/
│       ├── ui/
│       └── MainActivity.kt
│
└── docs/
    └── capturas/
```

---

# 🚀 Cómo ejecutarlo

## Requisitos

- .NET 10 SDK
- Android Studio
- Android 8.0 (API 26) o superior
- JDK 17 (recomendado Eclipse Temurin)

---

## 1️⃣ Ejecutar el servidor

```bash
cd server/GoodOffice.Api
dotnet run
```

La API quedará disponible en:

- `http://localhost:5080`
- `http://IP-DEL-PC:5080`

### Recursos disponibles

| Recurso | URL |
|----------|-----|
| Página de inicio | `http://localhost:5080/` |
| REST Docs | `http://localhost:5080/docs` |
| OpenAPI | `http://localhost:5080/openapi/v1.json` |
| SOAP WSDL | `http://localhost:5080/soap/sensores.asmx?wsdl` |

> La primera ejecución genera automáticamente la base de datos `goodoffice.db` con datos de demostración.

---

## 2️⃣ Ejecutar la aplicación Android

Abre la carpeta `android/` en Android Studio y espera la sincronización de Gradle.

### Configuración del servidor

**Emulador Android**

```text
http://10.0.2.2:5080/
```

**Teléfono físico**

```text
http://IP-DEL-PC:5080/
```

> Ambos dispositivos deben estar conectados a la misma red local.

---

> [!IMPORTANT]
>
> Utiliza **JDK 17** para Gradle.
>
> Algunas versiones recientes de Android Studio incluyen Java 25, lo que puede provocar errores de sincronización y compilación.

### Compilación desde terminal

```powershell
cd android

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17"

.\gradlew.bat assembleDebug
```

APK generado:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 👤 Usuario de prueba

| Correo | Contraseña |
|----------|------------|
| demo@goodoffice.local | goodoffice123 |

Usuarios adicionales:

- operaciones@goodoffice.local
- soporte@goodoffice.local
- rrhh@goodoffice.local
- finanzas@goodoffice.local

> Todos utilizan la misma contraseña de demostración.

---

# 🌐 Servicios web

## REST

Todos los endpoints (excepto login) requieren:

```http
Authorization: Bearer {token}
```

### Endpoints principales

| Método | Endpoint | Descripción |
|----------|-----------|-------------|
| POST | `/api/auth/login` | Iniciar sesión |
| GET | `/api/auth/yo` | Usuario autenticado |
| GET | `/api/usuarios` | Directorio de usuarios |
| GET | `/api/conversaciones` | Conversaciones |
| POST | `/api/conversaciones/{id}/mensajes` | Enviar mensaje |
| GET | `/api/documentos` | Listar documentos |
| POST | `/api/documentos` | Subir archivo |
| GET | `/api/reuniones` | Próximas reuniones |
| POST | `/api/reuniones` | Crear reunión |
| GET | `/api/sensores` | Estado ambiental |
| POST | `/api/sensores/lecturas` | Registrar lectura |
| GET | `/api/alertas` | Alertas |
| PUT | `/api/alertas/{id}/resolver` | Resolver alerta |
| GET | `/api/analisis` | KPIs y recomendaciones |

---

## SOAP

Servicio disponible en:

```text
http://localhost:5080/soap/sensores.asmx
```

WSDL:

```text
http://localhost:5080/soap/sensores.asmx?wsdl
```

Compatible con:

- Postman
- SoapUI
- Clientes SOAP tradicionales

---

# 🌡️ Sensores, alertas y autorregulación

## Sensores

### Luminosidad

Obtenida desde el sensor real del dispositivo Android:

```kotlin
Sensor.TYPE_LIGHT
```

Las lecturas son enviadas cada 10 segundos mediante:

```http
POST /api/sensores/lecturas
```

### Temperatura y humedad

Generadas por un simulador que opera en el servidor cada 30 segundos.

---

## Gestión de alertas

Cuando un valor supera los umbrales definidos:

✅ Se genera una alerta.

✅ Se clasifica como advertencia o crítica.

✅ Se propone una acción correctiva.

✅ Se cierra automáticamente cuando el valor vuelve a la normalidad.

### Rangos ideales

| Variable | Rango |
|-----------|---------|
| Temperatura | 20–24 °C |
| Humedad | 40–60 % |
| Luminosidad | 300–750 lx |

---

# 📧 Correo corporativo

Por defecto:

```json
"Correo": {
  "UsarSmtp": false
}
```

Las invitaciones se almacenan como archivos `.eml` en:

```text
server/GoodOffice.Api/correos/
```

Cada correo incluye una invitación de calendario en formato `.ics`.


---

# ⚙️ Configuración

Parámetros principales de `appsettings.json`:

| Sección | Descripción |
|----------|------------|
| `Umbrales` | Rangos de confort y horario laboral |
| `Simulador` | Configuración de sensores simulados |
| `Correo` | SMTP o almacenamiento `.eml` |
| `ConnectionStrings` | Base de datos SQLite |
| `Urls` | Host y puerto de la API |

---

## 📄 Contexto académico

> Proyecto académico desarrollado para el curso **Programación de aplicaciones móviles I**.
>
> Los usuarios, conversaciones, correos, documentos y datos incluidos son completamente ficticios y se utilizan únicamente con fines educativos.

---

<p align="center">
  <strong>Good Office</strong><br>
  Comunicación interna y monitoreo inteligente del entorno laboral 📈🏢
</p>
