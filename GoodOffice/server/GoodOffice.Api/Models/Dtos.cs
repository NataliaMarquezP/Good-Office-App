namespace GoodOffice.Api.Models;

// Objetos de entrada y salida de la API. Son clases con setters
// para que funcionen tanto con JSON como con XML.

public class LoginRequest
{
    public string Correo { get; set; } = "";
    public string Clave { get; set; } = "";
}

public class LoginResponse
{
    public string Token { get; set; } = "";
    public Usuario Usuario { get; set; } = new();
}

public class ConversacionResumen
{
    public int Id { get; set; }
    public string Nombre { get; set; } = "";
    public bool EsGrupo { get; set; }
    public string UltimoMensaje { get; set; } = "";
    public DateTime? UltimaFecha { get; set; }
}

public class MensajeDto
{
    public int Id { get; set; }
    public int ConversacionId { get; set; }
    public int UsuarioId { get; set; }
    public string Autor { get; set; } = "";
    public string Texto { get; set; } = "";
    public DateTime EnviadoEn { get; set; }
    public bool EsMio { get; set; }
    public Documento? Documento { get; set; }
}

public class NuevoMensaje
{
    public string Texto { get; set; } = "";
    public int? DocumentoId { get; set; }
}

public class NuevaReunion
{
    public string Titulo { get; set; } = "";
    public string Descripcion { get; set; } = "";
    public DateTime Inicio { get; set; }
    public int DuracionMinutos { get; set; } = 30;
    public string Sala { get; set; } = "";
    public List<string> Invitados { get; set; } = new();
    public bool EnviarCorreo { get; set; } = true;
}

public class NuevaLectura
{
    public string Sala { get; set; } = "";
    public string Tipo { get; set; } = "";
    public double Valor { get; set; }
    public string Origen { get; set; } = "telefono";
}

public class EstadoSensor
{
    public string Tipo { get; set; } = "";
    public double Valor { get; set; }
    public string Unidad { get; set; } = "";
    public double Minimo { get; set; }
    public double Maximo { get; set; }
    /// <summary>ok | advertencia | critica</summary>
    public string Estado { get; set; } = "ok";
    public string Origen { get; set; } = "";
    public DateTime FechaHora { get; set; }
}

public class AmbienteSala
{
    public string Sala { get; set; } = "";
    public List<EstadoSensor> Sensores { get; set; } = new();
}

public class PuntoSerie
{
    public DateTime FechaHora { get; set; }
    public double Valor { get; set; }
}

public class Kpi
{
    public string Nombre { get; set; } = "";
    public string Valor { get; set; } = "";
    public string Detalle { get; set; } = "";
}

public class Analisis
{
    public List<Kpi> Kpis { get; set; } = new();
    public List<PuntoSerie> MensajesPorDia { get; set; } = new();
    public List<PuntoSerie> TemperaturaPromedioPorDia { get; set; } = new();
    public List<Kpi> ConfortPorSala { get; set; } = new();
    public List<string> Recomendaciones { get; set; } = new();
}
