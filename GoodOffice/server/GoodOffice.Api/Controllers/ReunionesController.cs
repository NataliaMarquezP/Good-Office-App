using GoodOffice.Api.Data;
using GoodOffice.Api.Models;
using GoodOffice.Api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace GoodOffice.Api.Controllers;

[ApiController]
[Route("api/reuniones")]
public class ReunionesController(GoodOfficeDb db, CorreoService correo) : ControllerBase
{
    /// <summary>Reuniones desde hoy (o desde la fecha indicada).</summary>
    [HttpGet]
    public async Task<List<Reunion>> Listar([FromQuery] DateTime? desde)
    {
        var inicio = desde ?? DateTime.Today;
        return await db.Reuniones.Where(r => r.Inicio >= inicio).OrderBy(r => r.Inicio).ToListAsync();
    }

    [HttpGet("{id:int}")]
    public async Task<ActionResult<Reunion>> Obtener(int id) =>
        await db.Reuniones.FindAsync(id) is { } r ? r : NotFound();

    /// <summary>Agenda una reunión y envía la invitación por correo (con archivo .ics).</summary>
    [HttpPost]
    public async Task<ActionResult<Reunion>> Crear(NuevaReunion n)
    {
        if (string.IsNullOrWhiteSpace(n.Titulo)) return BadRequest(new { error = "Falta el título" });
        if (n.Inicio < DateTime.Now.AddMinutes(-5)) return BadRequest(new { error = "La fecha ya pasó" });

        var fin = n.Inicio.AddMinutes(n.DuracionMinutos);
        var choque = await db.Reuniones.Where(r => r.Sala == n.Sala && r.Inicio < fin).ToListAsync();
        if (choque.Any(r => r.Inicio.AddMinutes(r.DuracionMinutos) > n.Inicio))
            return Conflict(new { error = $"La sala {n.Sala} ya está ocupada en ese horario" });

        var org = HttpContext.UsuarioActual();
        var invitados = n.Invitados.Select(i => i.Trim().ToLowerInvariant()).Where(i => i != "").Distinct().ToList();
        var r = new Reunion
        {
            Titulo = n.Titulo.Trim(), Descripcion = n.Descripcion, Inicio = n.Inicio,
            DuracionMinutos = n.DuracionMinutos, Sala = n.Sala, OrganizadorId = org.Id,
            Invitados = string.Join(",", invitados)
        };
        db.Reuniones.Add(r);
        await db.SaveChangesAsync();

        if (n.EnviarCorreo)
        {
            r.CorreoEnviado = await correo.EnviarInvitacionAsync(r, org, invitados);
            await db.SaveChangesAsync();
        }
        return CreatedAtAction(nameof(Obtener), new { id = r.Id }, r);
    }

    [HttpDelete("{id:int}")]
    public async Task<IActionResult> Eliminar(int id)
    {
        var r = await db.Reuniones.FindAsync(id);
        if (r is null) return NotFound();
        db.Reuniones.Remove(r);
        await db.SaveChangesAsync();
        return NoContent();
    }
}
