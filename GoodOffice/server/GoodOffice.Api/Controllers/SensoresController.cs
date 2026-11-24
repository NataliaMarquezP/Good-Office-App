using GoodOffice.Api.Data;
using GoodOffice.Api.Models;
using GoodOffice.Api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace GoodOffice.Api.Controllers;

/// <summary>
/// Sensores de ambiente. Responde JSON por defecto y XML si se envía
/// el encabezado "Accept: application/xml".
/// </summary>
[ApiController]
[Route("api/sensores")]
[Produces("application/json", "application/xml")]
public class SensoresController(SensorService sensores) : ControllerBase
{
    /// <summary>Salas que tienen sensores.</summary>
    [HttpGet("salas")]
    public Task<List<string>> Salas() => sensores.SalasAsync();

    /// <summary>Última lectura de temperatura, humedad y luminosidad de cada sala (o de una).</summary>
    [HttpGet]
    public async Task<List<AmbienteSala>> Actual([FromQuery] string? sala)
    {
        var salas = sala is null ? await sensores.SalasAsync() : [sala];
        var res = new List<AmbienteSala>();
        foreach (var s in salas) res.Add(await sensores.AmbienteAsync(s));
        return res;
    }

    /// <summary>Historial de un sensor para graficar (por defecto 24 horas).</summary>
    [HttpGet("historial")]
    public async Task<ActionResult<List<PuntoSerie>>> Historial([FromQuery] string sala, [FromQuery] string tipo, [FromQuery] int horas = 24)
    {
        if (!UmbralesOptions.Tipos.Contains(tipo)) return BadRequest(new { error = "tipo debe ser temperatura, humedad o luminosidad" });
        return await sensores.HistorialAsync(sala, tipo, Math.Clamp(horas, 1, 24 * 30));
    }

    /// <summary>Registra una lectura. La app la usa para enviar el sensor de luz del teléfono.</summary>
    [HttpPost("lecturas")]
    public async Task<ActionResult<LecturaSensor>> Registrar(NuevaLectura n)
    {
        if (!UmbralesOptions.Tipos.Contains(n.Tipo)) return BadRequest(new { error = "tipo debe ser temperatura, humedad o luminosidad" });
        if (string.IsNullOrWhiteSpace(n.Sala)) return BadRequest(new { error = "Falta la sala" });
        var l = await sensores.RegistrarAsync(n.Sala, n.Tipo, n.Valor, string.IsNullOrWhiteSpace(n.Origen) ? "telefono" : n.Origen);
        return Created($"/api/sensores/historial?sala={Uri.EscapeDataString(l.Sala)}&tipo={l.Tipo}", l);
    }
}

[ApiController]
[Route("api/alertas")]
[Produces("application/json", "application/xml")]
public class AlertasController(GoodOfficeDb db) : ControllerBase
{
    /// <summary>Alertas de ambiente. Por defecto solo las activas; con todas=true también las resueltas.</summary>
    [HttpGet]
    public async Task<List<Alerta>> Listar([FromQuery] bool todas = false) =>
        await db.Alertas.Where(a => todas || !a.Resuelta).OrderByDescending(a => a.FechaHora).Take(100).ToListAsync();

    /// <summary>Marca una alerta como atendida.</summary>
    [HttpPut("{id:int}/resolver")]
    public async Task<ActionResult<Alerta>> Resolver(int id)
    {
        var a = await db.Alertas.FindAsync(id);
        if (a is null) return NotFound();
        a.Resuelta = true;
        await db.SaveChangesAsync();
        return a;
    }
}
