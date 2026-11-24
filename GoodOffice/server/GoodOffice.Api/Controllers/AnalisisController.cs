using System.Globalization;
using GoodOffice.Api.Data;
using GoodOffice.Api.Models;
using GoodOffice.Api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace GoodOffice.Api.Controllers;

/// <summary>
/// Análisis de datos: KPIs de uso de la app y de confort del ambiente,
/// con recomendaciones automáticas (mejora continua).
/// </summary>
[ApiController]
[Route("api/analisis")]
public class AnalisisController(GoodOfficeDb db, IOptions<UmbralesOptions> umbrales) : ControllerBase
{
    private static readonly CultureInfo Es = new("es-CL");

    [HttpGet]
    public async Task<Analisis> Obtener([FromQuery] int dias = 7)
    {
        dias = Math.Clamp(dias, 1, 30);
        var u = umbrales.Value;
        var desde = DateTime.Today.AddDays(-(dias - 1));
        var res = new Analisis();

        // --- Uso de la comunicación interna ---
        var mensajes = await db.Mensajes.Where(m => m.EnviadoEn >= desde).Select(m => m.EnviadoEn).ToListAsync();
        res.MensajesPorDia = Enumerable.Range(0, dias).Select(i => desde.AddDays(i))
            .Select(d => new PuntoSerie { FechaHora = d, Valor = mensajes.Count(m => m.Date == d) }).ToList();
        var horaPico = mensajes.GroupBy(m => m.Hour).OrderByDescending(g => g.Count()).Select(g => g.Key).FirstOrDefault();

        var reunionesProx = await db.Reuniones.CountAsync(r => r.Inicio >= DateTime.Now && r.Inicio < DateTime.Now.AddDays(7));
        var docs = await db.Documentos.CountAsync(d => d.SubidoEn >= desde);

        // --- Ambiente ---
        var lecturas = await db.Lecturas.Where(l => l.FechaHora >= desde).ToListAsync();
        double PctOk(IEnumerable<LecturaSensor> ls)
        {
            var lista = ls.ToList();
            return lista.Count == 0 ? 0 : 100.0 * lista.Count(l => u.Clasificar(l.Tipo, l.Valor, l.FechaHora) == "ok") / lista.Count;
        }
        var confortGeneral = PctOk(lecturas);
        var porSala = lecturas.GroupBy(l => l.Sala)
            .Select(g => new { Sala = g.Key, Pct = PctOk(g), Peor = g.GroupBy(x => x.Tipo).OrderBy(t => PctOk(t)).First().Key })
            .OrderBy(x => x.Pct).ToList();
        res.ConfortPorSala = porSala.Select(s => new Kpi
        {
            Nombre = s.Sala, Valor = $"{s.Pct:0}%", Detalle = $"Variable más fuera de rango: {s.Peor}"
        }).ToList();

        res.TemperaturaPromedioPorDia = lecturas.Where(l => l.Tipo == "temperatura")
            .GroupBy(l => l.FechaHora.Date).OrderBy(g => g.Key)
            .Select(g => new PuntoSerie { FechaHora = g.Key, Valor = Math.Round(g.Average(x => x.Valor), 1) }).ToList();

        var alertas = await db.Alertas.Where(a => a.FechaHora >= desde).ToListAsync();

        res.Kpis =
        [
            new() { Nombre = "Mensajes", Valor = mensajes.Count.ToString(), Detalle = $"{mensajes.Count / (double)dias:0.0} por día · hora pico {horaPico}:00" },
            new() { Nombre = "Reuniones próximas", Valor = reunionesProx.ToString(), Detalle = "Próximos 7 días" },
            new() { Nombre = "Documentos compartidos", Valor = docs.ToString(), Detalle = $"Últimos {dias} días" },
            new() { Nombre = "Confort del ambiente", Valor = $"{confortGeneral:0}%", Detalle = "Lecturas dentro del rango ideal" },
            new() { Nombre = "Alertas", Valor = alertas.Count.ToString(), Detalle = $"{alertas.Count(a => a.Nivel == "critica")} críticas · {alertas.Count(a => !a.Resuelta)} activas" },
        ];

        // --- Recomendaciones (autorregulación / mejora continua) ---
        var peor = porSala.FirstOrDefault();
        if (peor is not null && peor.Pct < 80)
            res.Recomendaciones.Add($"{peor.Sala} está en rango solo el {peor.Pct:0}% del tiempo (sobre todo {peor.Peor}). Revisar climatización o evitar agendar reuniones largas ahí en las horas críticas.");
        var tempAlta = lecturas.Where(l => l.Tipo == "temperatura" && l.Valor > u.Temperatura.Maximo)
            .GroupBy(l => l.FechaHora.Hour).OrderByDescending(g => g.Count()).FirstOrDefault();
        if (tempAlta is not null)
            res.Recomendaciones.Add($"La temperatura supera {u.Temperatura.Maximo} °C con más frecuencia cerca de las {tempAlta.Key}:00. Programar el aire acondicionado 30 minutos antes.");
        var luzBaja = lecturas.Where(l => l.Tipo == "luminosidad" && l.FechaHora.Hour is >= 8 and <= 19 && l.Valor < u.Luminosidad.Minimo).ToList();
        if (luzBaja.Count > 0)
            res.Recomendaciones.Add($"Hubo {luzBaja.Count} lecturas con poca luz en horario laboral, sobre todo en {luzBaja.GroupBy(l => l.Sala).OrderByDescending(g => g.Count()).First().Key}.");
        if (mensajes.Count > 0)
            res.Recomendaciones.Add($"El mayor uso del chat es a las {horaPico}:00. Conviene evitar reuniones en esa hora para no cortar la coordinación del equipo.");
        if (res.Recomendaciones.Count == 0)
            res.Recomendaciones.Add("Todos los indicadores están dentro de lo esperado.");

        return res;
    }
}
