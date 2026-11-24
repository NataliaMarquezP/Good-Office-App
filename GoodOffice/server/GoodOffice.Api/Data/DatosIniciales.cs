using System.Security.Cryptography;
using System.Text;
using GoodOffice.Api.Models;
using GoodOffice.Api.Services;

namespace GoodOffice.Api.Data;

/// <summary>Crea la base SQLite y la llena con datos de ejemplo la primera vez.</summary>
public static class DatosIniciales
{
    public const string ClaveDemo = "goodoffice123";

    public static string Hash(string clave) =>
        Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(clave)));

    public static async Task CargarAsync(IServiceProvider sp)
    {
        using var scope = sp.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<GoodOfficeDb>();
        await db.Database.EnsureCreatedAsync();
        if (db.Usuarios.Any()) return;

        var rnd = new Random(7);
        var ahora = DateTime.Now;
        var hash = Hash(ClaveDemo);

        // Usuarios genéricos (sin nombres de personas reales).
        var usuarios = new List<Usuario>
        {
            new() { Nombre = "Usuario Demo", Correo = "demo@goodoffice.local", Cargo = "Analista", Area = "Operaciones", ClaveHash = hash },
            new() { Nombre = "Jefatura de Operaciones", Correo = "operaciones@goodoffice.local", Cargo = "Jefe de área", Area = "Operaciones", ClaveHash = hash },
            new() { Nombre = "Soporte TI", Correo = "soporte@goodoffice.local", Cargo = "Técnico", Area = "Tecnología", ClaveHash = hash },
            new() { Nombre = "Recursos Humanos", Correo = "rrhh@goodoffice.local", Cargo = "Coordinación", Area = "Personas", ClaveHash = hash },
            new() { Nombre = "Finanzas", Correo = "finanzas@goodoffice.local", Cargo = "Analista", Area = "Finanzas", ClaveHash = hash },
        };
        db.Usuarios.AddRange(usuarios);
        await db.SaveChangesAsync();

        var convs = new List<Conversacion>
        {
            new() { Nombre = "General", EsGrupo = true },
            new() { Nombre = "Equipo de Operaciones", EsGrupo = true },
            new() { Nombre = "Soporte TI", EsGrupo = false },
        };
        db.Conversaciones.AddRange(convs);
        await db.SaveChangesAsync();

        // Documentos de ejemplo guardados como archivos en la carpeta "almacen".
        var almacen = Path.GetFullPath("almacen");
        Directory.CreateDirectory(almacen);
        var docs = new List<Documento>();
        foreach (var (nombre, contenido, conv, autor) in new[]
        {
            ("Protocolo de ventilacion.txt", "Abrir ventanas 10 minutos cada 2 horas. Mantener la temperatura entre 20 y 24 °C.", 1, 1),
            ("Informe mensual de operaciones.txt", "Resumen de indicadores del mes: tickets resueltos, reuniones y uso de salas.", 0, 1),
            ("Manual de uso de salas.txt", "Reservar con anticipación desde la app. Dejar la sala ordenada y las luces apagadas.", 0, 3),
        })
        {
            var ruta = Path.Combine(almacen, Guid.NewGuid() + ".txt");
            await File.WriteAllTextAsync(ruta, contenido);
            docs.Add(new Documento
            {
                Nombre = nombre, TipoMime = "text/plain", TamanoBytes = new FileInfo(ruta).Length, RutaArchivo = ruta,
                SubidoPorId = usuarios[autor].Id, ConversacionId = convs[conv].Id, SubidoEn = ahora.AddDays(-rnd.Next(1, 5))
            });
        }
        db.Documentos.AddRange(docs);
        await db.SaveChangesAsync();

        // Mensajes de los últimos 7 días (sirven para los KPIs de uso).
        string[] textos =
        [
            "Buenos días equipo, ¿cómo vamos con los pendientes?",
            "Ya subí el informe a la carpeta compartida.",
            "La sala de reuniones 1 está muy calurosa, ¿podemos cambiarnos?",
            "Recuerden la reunión de las 11:00.",
            "Listo, quedó resuelto el ticket del proyector.",
            "¿Alguien tiene el acta de la reunión anterior?",
            "Gracias por la ayuda.",
            "Voy en camino a la oficina.",
        ];
        var mensajes = new List<Mensaje>();
        for (var d = 6; d >= 0; d--)
        {
            var cantidad = rnd.Next(4, 12);
            for (var i = 0; i < cantidad; i++)
            {
                var conv = convs[rnd.Next(convs.Count)];
                var fecha = ahora.Date.AddDays(-d).AddHours(rnd.Next(8, 19)).AddMinutes(rnd.Next(60));
                if (fecha > ahora) fecha = ahora.AddMinutes(-rnd.Next(5, 120));
                mensajes.Add(new Mensaje
                {
                    ConversacionId = conv.Id,
                    UsuarioId = usuarios[rnd.Next(usuarios.Count)].Id,
                    Texto = textos[rnd.Next(textos.Length)],
                    EnviadoEn = fecha
                });
            }
        }
        mensajes.Add(new Mensaje
        {
            ConversacionId = convs[1].Id, UsuarioId = usuarios[1].Id, Texto = "Les comparto el protocolo de ventilación.",
            DocumentoId = docs[0].Id, EnviadoEn = ahora.AddHours(-1)
        });
        db.Mensajes.AddRange(mensajes);

        db.Reuniones.AddRange(
            new Reunion { Titulo = "Reunión semanal de operaciones", Descripcion = "Revisión de indicadores", Inicio = ahora.Date.AddDays(1).AddHours(10), DuracionMinutos = 45, Sala = "Sala de reuniones 1", OrganizadorId = usuarios[1].Id, Invitados = "demo@goodoffice.local,soporte@goodoffice.local", CorreoEnviado = true },
            new Reunion { Titulo = "Capacitación de la app", Descripcion = "Uso de Good Office para todo el equipo", Inicio = ahora.Date.AddDays(2).AddHours(15), DuracionMinutos = 60, Sala = "Sala de capacitación", OrganizadorId = usuarios[3].Id, Invitados = "demo@goodoffice.local,finanzas@goodoffice.local", CorreoEnviado = true },
            new Reunion { Titulo = "Cierre de mes", Descripcion = "", Inicio = ahora.Date.AddDays(-1).AddHours(16), DuracionMinutos = 30, Sala = "Oficina abierta", OrganizadorId = usuarios[4].Id, Invitados = "demo@goodoffice.local", CorreoEnviado = true }
        );
        await db.SaveChangesAsync();

        // Historial de sensores: una lectura por hora durante 7 días.
        var lecturas = new List<LecturaSensor>();
        var umbrales = new UmbralesOptions();
        for (var t = ahora.AddDays(-7); t < ahora.AddHours(-1); t = t.AddHours(1))
            foreach (var sala in SimuladorSensores.Salas)
                foreach (var tipo in UmbralesOptions.Tipos)
                    lecturas.Add(new LecturaSensor
                    {
                        Sala = sala, Tipo = tipo, Origen = "simulador", FechaHora = t,
                        Valor = Math.Round(SimuladorSensores.Valor(sala, tipo, t, rnd), 1),
                        Unidad = umbrales.Para(tipo)!.Unidad
                    });
        db.Lecturas.AddRange(lecturas);

        // Alertas históricas (ya resueltas) para la pantalla de alertas y el análisis.
        db.Alertas.AddRange(
            new Alerta { Sala = "Sala de reuniones 1", Tipo = "temperatura", Nivel = "critica", Valor = 27.8, FechaHora = ahora.AddDays(-2).Date.AddHours(16), Resuelta = true,
                Mensaje = "Temperatura sobre el rango ideal en Sala de reuniones 1: 27,8 °C (ideal 20-24 °C).", Sugerencia = "Encender el aire acondicionado o ventilar la sala." },
            new Alerta { Sala = "Sala de capacitación", Tipo = "luminosidad", Nivel = "advertencia", Valor = 210, FechaHora = ahora.AddDays(-1).Date.AddHours(9), Resuelta = true,
                Mensaje = "Luminosidad bajo el rango ideal en Sala de capacitación: 210 lx (ideal 300-750 lx).", Sugerencia = "Encender luces del sector o abrir cortinas." }
        );
        await db.SaveChangesAsync();
    }
}
