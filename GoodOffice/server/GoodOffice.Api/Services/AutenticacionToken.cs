using GoodOffice.Api.Data;
using GoodOffice.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace GoodOffice.Api.Services;

/// <summary>
/// Autenticación simple por token (encabezado "Authorization: Bearer {token}").
/// El token se obtiene en POST /api/auth/login. Para una tarea es suficiente;
/// en producción se usaría OAuth 2.0 / JWT con el directorio de la empresa.
/// </summary>
public class AutenticacionToken(RequestDelegate next)
{
    public async Task InvokeAsync(HttpContext ctx, GoodOfficeDb db)
    {
        var ruta = ctx.Request.Path;
        var protegida = ruta.StartsWithSegments("/api") && !ruta.StartsWithSegments("/api/auth/login");
        if (!protegida)
        {
            await next(ctx);
            return;
        }

        var header = ctx.Request.Headers.Authorization.ToString();
        var token = header.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase) ? header[7..].Trim() : "";
        var sesion = token == "" ? null : await db.Sesiones.AsNoTracking().FirstOrDefaultAsync(s => s.Token == token);
        var usuario = sesion is null ? null : await db.Usuarios.AsNoTracking().FirstOrDefaultAsync(u => u.Id == sesion.UsuarioId);
        if (usuario is null)
        {
            ctx.Response.StatusCode = StatusCodes.Status401Unauthorized;
            await ctx.Response.WriteAsJsonAsync(new { error = "Falta el token o no es válido. Inicia sesión en /api/auth/login." });
            return;
        }

        ctx.Items["usuario"] = usuario;
        await next(ctx);
    }
}

public static class HttpContextExtensions
{
    public static Usuario UsuarioActual(this HttpContext ctx) => (Usuario)ctx.Items["usuario"]!;
}
