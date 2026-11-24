namespace GoodOffice.Api.Services;

/// <summary>
/// Simula sensores de temperatura y humedad (por ejemplo un DHT22 en cada sala).
/// Cada cierto tiempo genera una lectura nueva por sala. La luminosidad
/// la manda el teléfono con su sensor de luz real; si ningún teléfono
/// la envía, también se simula para que el panel no quede vacío.
/// </summary>
public class SimuladorSensores(IServiceScopeFactory scopes, IConfiguration config, ILogger<SimuladorSensores> log) : BackgroundService
{
    public static readonly string[] Salas = ["Oficina abierta", "Sala de reuniones 1", "Sala de capacitación"];

    /// <summary>Valor "realista" según la hora del día. Lo usa también el generador de datos iniciales.</summary>
    public static double Valor(string sala, string tipo, DateTime t, Random rnd)
    {
        var h = t.Hour + t.Minute / 60.0;
        var dia = Math.Sin((h - 9) / 24 * 2 * Math.PI); // máximo a media tarde
        var oficina = h is >= 8 and <= 19;
        // La sala de reuniones 1 se calienta más (tiene ventanal al poniente): así hay alertas para analizar.
        var extra = sala == "Sala de reuniones 1" ? 1.8 : sala == "Sala de capacitación" ? -0.6 : 0;
        return tipo switch
        {
            "temperatura" => 21.5 + 2.2 * dia + extra + (rnd.NextDouble() - 0.5) * 1.2,
            "humedad" => 50 - 8 * dia + (rnd.NextDouble() - 0.5) * 6 + (sala == "Sala de capacitación" ? 6 : 0),
            "luminosidad" => oficina ? 480 + 180 * Math.Sin((h - 8) / 11 * Math.PI) + (rnd.NextDouble() - 0.5) * 160 - (sala == "Sala de capacitación" ? 220 : 0) : 15 + rnd.NextDouble() * 20,
            _ => 0
        };
    }

    protected override async Task ExecuteAsync(CancellationToken ct)
    {
        var segundos = config.GetValue("Simulador:IntervaloSegundos", 30);
        if (!config.GetValue("Simulador:Habilitado", true)) return;
        var rnd = new Random();
        log.LogInformation("Simulador de sensores activo, una lectura cada {s} s", segundos);

        while (!ct.IsCancellationRequested)
        {
            try
            {
                using var scope = scopes.CreateScope();
                var sensores = scope.ServiceProvider.GetRequiredService<SensorService>();
                var db = scope.ServiceProvider.GetRequiredService<Data.GoodOfficeDb>();
                var ahora = DateTime.Now;
                foreach (var sala in Salas)
                {
                    await sensores.RegistrarAsync(sala, "temperatura", Valor(sala, "temperatura", ahora, rnd), "simulador");
                    await sensores.RegistrarAsync(sala, "humedad", Valor(sala, "humedad", ahora, rnd), "simulador");

                    var hayTelefono = db.Lecturas.Any(l => l.Sala == sala && l.Tipo == "luminosidad"
                        && l.Origen == "telefono" && l.FechaHora > ahora.AddMinutes(-2));
                    if (!hayTelefono)
                        await sensores.RegistrarAsync(sala, "luminosidad", Valor(sala, "luminosidad", ahora, rnd), "simulador");
                }
            }
            catch (Exception ex)
            {
                log.LogWarning(ex, "Error en el simulador de sensores");
            }
            await Task.Delay(TimeSpan.FromSeconds(segundos), ct);
        }
    }
}
