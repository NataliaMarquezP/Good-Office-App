using GoodOffice.Api.Data;
using GoodOffice.Api.Models;
using GoodOffice.Api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace GoodOffice.Api.Controllers;

[ApiController]
[Route("api")]
public class AuthController(GoodOfficeDb db) : ControllerBase
{
    /// <summary>Inicia sesión con correo corporativo y clave. Devuelve el token para las demás llamadas.</summary>
    [HttpPost("auth/login")]
    public async Task<ActionResult<LoginResponse>> Login(LoginRequest req)
    {
        var correo = req.Correo.Trim().ToLowerInvariant();
        var usuario = await db.Usuarios.FirstOrDefaultAsync(u => u.Correo == correo);
        if (usuario is null || usuario.ClaveHash != DatosIniciales.Hash(req.Clave))
            return Unauthorized(new { error = "Correo o clave incorrectos" });

        var sesion = new Sesion { Token = Guid.NewGuid().ToString("N"), UsuarioId = usuario.Id, CreadaEn = DateTime.Now };
        db.Sesiones.Add(sesion);
        await db.SaveChangesAsync();
        return new LoginResponse { Token = sesion.Token, Usuario = usuario };
    }

    /// <summary>Datos del usuario que inició sesión.</summary>
    [HttpGet("auth/yo")]
    public Usuario Yo() => HttpContext.UsuarioActual();

    /// <summary>Directorio de usuarios (para elegir invitados).</summary>
    [HttpGet("usuarios")]
    public async Task<List<Usuario>> Usuarios() => await db.Usuarios.OrderBy(u => u.Nombre).ToListAsync();
}
