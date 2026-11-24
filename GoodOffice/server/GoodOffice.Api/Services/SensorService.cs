using GoodOffice.Api.Data;
using GoodOffice.Api.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace GoodOffice.Api.Services;

/// <summary>
/// Lógica de sensores: guarda lecturas, compara con los umbrales
/// y genera alertas con una sugerencia de acción.
/// La usan el controlador REST, el servicio SOAP y el simulador.
/// </summary>
public class SensorService(GoodOfficeDb db, IOptions<UmbralesOptions> umbrales)
{
    private readonly UmbralesOptions _u = umbrales.Value;

    public async Task<LecturaSensor> RegistrarAsync(string sala, string tipo, double valor, string origen, DateTime? fecha = null)
    {
        tipo = tipo.Trim().ToLowerInvariant();
        var rango = _u.Para(tipo) ?? throw new ArgumentException($"Tipo de sensor no válido: {tipo}");
        var lectura = new LecturaSensor
        {
            Sala = sala,
            Tipo = tipo,
            Valor = Math.Round(valor, 1),
            Unidad = rango.Unidad,
            Origen = origen,
            FechaHora = fecha ?? DateTime.Now
        };
        db.Lecturas.Add(lectura);
        await EvaluarAsync(lectura);
        await db.SaveChangesAsync();
        return lectura;
    }

    private async Task EvaluarAsync(LecturaSensor l)
    {
        var nivel = _u.Clasificar(l.Tipo, l.Valor, l.FechaHora);
        var abierta = await db.Alertas
            .Where(a => a.Sala == l.Sala && a.Tipo == l.Tipo && !a.Resuelta)
            .OrderByDescending(a => a.FechaHora)
            .FirstOrDefaultAsync();

        if (nivel == "ok")
        {
            // Autorregulación: si el valor volvió al rango, se cierra la alerta.
            if (abierta is not null) abierta.Resuelta = true;
            return;
        }

        // Evita duplicar: si ya hay una alerta abierta del mismo nivel, solo se actualiza el valor.
        if (abierta is not null && abierta.Nivel == nivel)
        {
            abierta.Valor = l.Valor;
            return;
        }
        if (abierta is not null) abierta.Resuelta = true;

        var r = _u.Para(l.Tipo)!;
        var bajo = l.Valor < r.Minimo;
        db.Alertas.Add(new Alerta
        {
            Sala = l.Sala,
            Tipo = l.Tipo,
            Nivel = nivel,
            Valor = l.Valor,
            FechaHora = l.FechaHora,
            Mensaje = $"{Capitalizar(l.Tipo)} {(bajo ? "bajo" : "sobre")} el rango ideal en {l.Sala}: {l.Valor} {r.Unidad} (ideal {r.Minimo}-{r.Maximo} {r.Unidad}).",
            Sugerencia = Sugerir(l.Tipo, bajo)
        });
    }

    private static string Sugerir(string tipo, bool bajo) => (tipo, bajo) switch
    {
        ("temperatura", false) => "Encender el aire acondicionado o ventilar la sala. Considerar mover la reunión a otra sala.",
        ("temperatura", true) => "Subir la calefacción y revisar ventanas abiertas.",
        ("humedad", false) => "Ventilar o activar el deshumidificador.",
        ("humedad", true) => "Usar humidificador y recordar al equipo hidratarse.",
        ("luminosidad", true) => "Encender luces del sector o abrir cortinas para evitar fatiga visual.",
        ("luminosidad", false) => "Bajar cortinas o reducir la iluminación para evitar reflejos en pantallas.",
        _ => "Revisar el sector."
    };

    private static string Capitalizar(string s) => char.ToUpper(s[0]) + s[1..];

    public async Task<List<string>> SalasAsync() =>
        await db.Lecturas.Select(l => l.Sala).Distinct().OrderBy(s => s).ToListAsync();

    public async Task<AmbienteSala> AmbienteAsync(string sala)
    {
        var res = new AmbienteSala { Sala = sala };
        foreach (var tipo in UmbralesOptions.Tipos)
        {
            var ultima = await db.Lecturas
                .Where(l => l.Sala == sala && l.Tipo == tipo)
                .OrderByDescending(l => l.FechaHora)
                .FirstOrDefaultAsync();
            if (ultima is null) continue;
            var r = _u.Para(tipo)!;
            res.Sensores.Add(new EstadoSensor
            {
                Tipo = tipo,
                Valor = ultima.Valor,
                Unidad = ultima.Unidad,
                Minimo = r.Minimo,
                Maximo = r.Maximo,
                Estado = _u.Clasificar(tipo, ultima.Valor, ultima.FechaHora),
                Origen = ultima.Origen,
                FechaHora = ultima.FechaHora
            });
        }
        return res;
    }

    public async Task<List<PuntoSerie>> HistorialAsync(string sala, string tipo, int horas)
    {
        var desde = DateTime.Now.AddHours(-horas);
        return await db.Lecturas
            .Where(l => l.Sala == sala && l.Tipo == tipo && l.FechaHora >= desde)
            .OrderBy(l => l.FechaHora)
            .Select(l => new PuntoSerie { FechaHora = l.FechaHora, Valor = l.Valor })
            .ToListAsync();
    }
}
