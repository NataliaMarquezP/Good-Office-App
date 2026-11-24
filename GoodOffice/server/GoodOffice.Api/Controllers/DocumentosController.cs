using GoodOffice.Api.Data;
using GoodOffice.Api.Models;
using GoodOffice.Api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace GoodOffice.Api.Controllers;

[ApiController]
[Route("api/documentos")]
public class DocumentosController(GoodOfficeDb db) : ControllerBase
{
    private const long MaxBytes = 20 * 1024 * 1024;

    /// <summary>Documentos compartidos, del más nuevo al más antiguo.</summary>
    [HttpGet]
    public async Task<List<Documento>> Listar() =>
        await db.Documentos.OrderByDescending(d => d.SubidoEn).ToListAsync();

    /// <summary>Sube un archivo (multipart/form-data, campo "archivo").</summary>
    [HttpPost]
    [RequestSizeLimit(MaxBytes)]
    public async Task<ActionResult<Documento>> Subir(IFormFile archivo, [FromForm] int? conversacionId)
    {
        if (archivo is null || archivo.Length == 0) return BadRequest(new { error = "Falta el archivo" });

        var almacen = Path.GetFullPath("almacen");
        Directory.CreateDirectory(almacen);
        var ruta = Path.Combine(almacen, Guid.NewGuid() + Path.GetExtension(archivo.FileName));
        await using (var fs = System.IO.File.Create(ruta))
            await archivo.CopyToAsync(fs);

        var doc = new Documento
        {
            Nombre = Path.GetFileName(archivo.FileName),
            TipoMime = string.IsNullOrEmpty(archivo.ContentType) ? "application/octet-stream" : archivo.ContentType,
            TamanoBytes = archivo.Length, RutaArchivo = ruta,
            SubidoPorId = HttpContext.UsuarioActual().Id, ConversacionId = conversacionId, SubidoEn = DateTime.Now
        };
        db.Documentos.Add(doc);
        await db.SaveChangesAsync();
        return CreatedAtAction(nameof(Descargar), new { id = doc.Id }, doc);
    }

    /// <summary>Descarga el archivo.</summary>
    [HttpGet("{id:int}/descarga")]
    public async Task<IActionResult> Descargar(int id)
    {
        var doc = await db.Documentos.FindAsync(id);
        if (doc is null || !System.IO.File.Exists(doc.RutaArchivo)) return NotFound();
        return PhysicalFile(doc.RutaArchivo, doc.TipoMime, doc.Nombre);
    }
}
