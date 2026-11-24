using GoodOffice.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace GoodOffice.Api.Data;

public class GoodOfficeDb(DbContextOptions<GoodOfficeDb> options) : DbContext(options)
{
    public DbSet<Usuario> Usuarios => Set<Usuario>();
    public DbSet<Sesion> Sesiones => Set<Sesion>();
    public DbSet<Conversacion> Conversaciones => Set<Conversacion>();
    public DbSet<Mensaje> Mensajes => Set<Mensaje>();
    public DbSet<Documento> Documentos => Set<Documento>();
    public DbSet<Reunion> Reuniones => Set<Reunion>();
    public DbSet<LecturaSensor> Lecturas => Set<LecturaSensor>();
    public DbSet<Alerta> Alertas => Set<Alerta>();

    protected override void OnModelCreating(ModelBuilder b)
    {
        b.Entity<Sesion>().HasKey(s => s.Token);
        b.Entity<Usuario>().HasIndex(u => u.Correo).IsUnique();
        b.Entity<LecturaSensor>().HasIndex(l => new { l.Sala, l.Tipo, l.FechaHora });
        b.Entity<Mensaje>().HasIndex(m => new { m.ConversacionId, m.EnviadoEn });
    }
}
