using GoodOffice.Api.Data;
using GoodOffice.Api.Models;
using GoodOffice.Api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace GoodOffice.Api.Controllers;

[ApiController]
[Route("api/conversaciones")]
public class MensajesController(GoodOfficeDb db) : ControllerBase
{
    /// <summary>Lista de conversaciones con su último mensaje.</summary>
    [HttpGet]
    public async Task<List<ConversacionResumen>> Listar()
    {
        var convs = await db.Conversaciones.ToListAsync();
        var lista = new List<ConversacionResumen>();
        foreach (var c in convs)
        {
            var ultimo = await db.Mensajes.Where(m => m.ConversacionId == c.Id)
                .OrderByDescending(m => m.EnviadoEn).FirstOrDefaultAsync();
            lista.Add(new ConversacionResumen
            {
                Id = c.Id, Nombre = c.Nombre, EsGrupo = c.EsGrupo,
                UltimoMensaje = ultimo?.Texto ?? "", UltimaFecha = ultimo?.EnviadoEn
            });
        }
        return lista.OrderByDescending(c => c.UltimaFecha).ToList();
    }

    /// <summary>Mensajes de una conversación. Con "desde" se piden solo los nuevos (la app consulta cada pocos segundos).</summary>
    [HttpGet("{id:int}/mensajes")]
    public async Task<ActionResult<List<MensajeDto>>> Mensajes(int id, [FromQuery] DateTime? desde)
    {
        if (!await db.Conversaciones.AnyAsync(c => c.Id == id)) return NotFound();
        var yo = HttpContext.UsuarioActual().Id;
        var q = db.Mensajes.Where(m => m.ConversacionId == id);
        if (desde is not null) q = q.Where(m => m.EnviadoEn > desde);
        var mensajes = await q.OrderBy(m => m.EnviadoEn).ToListAsync();

        var usuarios = await db.Usuarios.ToDictionaryAsync(u => u.Id, u => u.Nombre);
        var docIds = mensajes.Where(m => m.DocumentoId != null).Select(m => m.DocumentoId!.Value).ToList();
        var docs = await db.Documentos.Where(d => docIds.Contains(d.Id)).ToDictionaryAsync(d => d.Id);

        return mensajes.Select(m => new MensajeDto
        {
            Id = m.Id, ConversacionId = m.ConversacionId, UsuarioId = m.UsuarioId,
            Autor = usuarios.GetValueOrDefault(m.UsuarioId, "?"), Texto = m.Texto, EnviadoEn = m.EnviadoEn,
            EsMio = m.UsuarioId == yo,
            Documento = m.DocumentoId is int d && docs.TryGetValue(d, out var doc) ? doc : null
        }).ToList();
    }

    /// <summary>Envía un mensaje. Para adjuntar un documento, primero se sube en POST /api/documentos.</summary>
    [HttpPost("{id:int}/mensajes")]
    public async Task<ActionResult<Mensaje>> Enviar(int id, NuevoMensaje nuevo)
    {
        if (!await db.Conversaciones.AnyAsync(c => c.Id == id)) return NotFound();
        if (string.IsNullOrWhiteSpace(nuevo.Texto) && nuevo.DocumentoId is null)
            return BadRequest(new { error = "El mensaje está vacío" });

        var m = new Mensaje
        {
            ConversacionId = id, UsuarioId = HttpContext.UsuarioActual().Id,
            Texto = nuevo.Texto.Trim(), DocumentoId = nuevo.DocumentoId, EnviadoEn = DateTime.Now
        };
        db.Mensajes.Add(m);
        await db.SaveChangesAsync();
        return CreatedAtAction(nameof(Mensajes), new { id }, m);
    }
}
