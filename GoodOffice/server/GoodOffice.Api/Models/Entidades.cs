namespace GoodOffice.Api.Models;

// Entidades que se guardan en SQLite (Entity Framework Core).
// Se dejan sin propiedades de navegación para que se puedan
// devolver directo como JSON o XML sin ciclos.

public class Usuario
{
    public int Id { get; set; }
    public string Nombre { get; set; } = "";
    public string Correo { get; set; } = "";
    public string Cargo { get; set; } = "";
    public string Area { get; set; } = "";
    [System.Text.Json.Serialization.JsonIgnore]
    [System.Xml.Serialization.XmlIgnore]
    public string ClaveHash { get; set; } = "";
}

public class Sesion
{
    public string Token { get; set; } = "";
    public int UsuarioId { get; set; }
    public DateTime CreadaEn { get; set; }
}

public class Conversacion
{
    public int Id { get; set; }
    public string Nombre { get; set; } = "";
    public bool EsGrupo { get; set; }
}

public class Mensaje
{
    public int Id { get; set; }
    public int ConversacionId { get; set; }
    public int UsuarioId { get; set; }
    public string Texto { get; set; } = "";
    public int? DocumentoId { get; set; }
    public DateTime EnviadoEn { get; set; }
}

public class Documento
{
    public int Id { get; set; }
    public string Nombre { get; set; } = "";
    public string TipoMime { get; set; } = "application/octet-stream";
    public long TamanoBytes { get; set; }
    [System.Text.Json.Serialization.JsonIgnore]
    [System.Xml.Serialization.XmlIgnore]
    public string RutaArchivo { get; set; } = "";
    public int SubidoPorId { get; set; }
    public int? ConversacionId { get; set; }
    public DateTime SubidoEn { get; set; }
}

public class Reunion
{
    public int Id { get; set; }
    public string Titulo { get; set; } = "";
    public string Descripcion { get; set; } = "";
    public DateTime Inicio { get; set; }
    public int DuracionMinutos { get; set; } = 30;
    public string Sala { get; set; } = "";
    public int OrganizadorId { get; set; }
    /// <summary>Correos de los invitados separados por coma.</summary>
    public string Invitados { get; set; } = "";
    public bool CorreoEnviado { get; set; }
}

public class LecturaSensor
{
    public int Id { get; set; }
    public string Sala { get; set; } = "";
    /// <summary>temperatura | humedad | luminosidad</summary>
    public string Tipo { get; set; } = "";
    public double Valor { get; set; }
    public string Unidad { get; set; } = "";
    /// <summary>simulador | telefono</summary>
    public string Origen { get; set; } = "";
    public DateTime FechaHora { get; set; }
}

public class Alerta
{
    public int Id { get; set; }
    public string Sala { get; set; } = "";
    public string Tipo { get; set; } = "";
    /// <summary>advertencia | critica</summary>
    public string Nivel { get; set; } = "";
    public double Valor { get; set; }
    public string Mensaje { get; set; } = "";
    public string Sugerencia { get; set; } = "";
    public DateTime FechaHora { get; set; }
    public bool Resuelta { get; set; }
}
