namespace GoodOffice.Api.Services;

/// <summary>Rango ideal de un sensor (se lee de appsettings.json, sección "Umbrales").</summary>
public class Rango
{
    public double Minimo { get; set; }
    public double Maximo { get; set; }
    /// <summary>Cuánto puede salirse del rango antes de ser crítico.</summary>
    public double MargenCritico { get; set; }
    public string Unidad { get; set; } = "";
}

public class UmbralesOptions
{
    public Rango Temperatura { get; set; } = new() { Minimo = 20, Maximo = 24, MargenCritico = 3, Unidad = "°C" };
    public Rango Humedad { get; set; } = new() { Minimo = 40, Maximo = 60, MargenCritico = 10, Unidad = "%" };
    public Rango Luminosidad { get; set; } = new() { Minimo = 300, Maximo = 750, MargenCritico = 150, Unidad = "lx" };

    /// <summary>Horario laboral: fuera de él no se exige luz (la oficina está vacía).</summary>
    public int HoraInicio { get; set; } = 8;
    public int HoraFin { get; set; } = 19;

    public Rango? Para(string tipo) => tipo switch
    {
        "temperatura" => Temperatura,
        "humedad" => Humedad,
        "luminosidad" => Luminosidad,
        _ => null
    };

    public static readonly string[] Tipos = ["temperatura", "humedad", "luminosidad"];

    /// <summary>Devuelve ok, advertencia o critica según el valor.</summary>
    public string Clasificar(string tipo, double valor, DateTime? fecha = null)
    {
        var r = Para(tipo);
        if (r is null) return "ok";
        if (tipo == "luminosidad" && fecha is { } f && (f.Hour < HoraInicio || f.Hour >= HoraFin)) return "ok";
        if (valor >= r.Minimo && valor <= r.Maximo) return "ok";
        var desvio = valor < r.Minimo ? r.Minimo - valor : valor - r.Maximo;
        return desvio > r.MargenCritico ? "critica" : "advertencia";
    }
}
