using System.Net;
using System.Net.Mail;
using System.Net.Mime;
using System.Text;
using GoodOffice.Api.Models;

namespace GoodOffice.Api.Services;

public class CorreoOptions
{
    /// <summary>true: envía por SMTP. false: guarda el correo como archivo .eml en CarpetaSalida.</summary>
    public bool UsarSmtp { get; set; }
    public string Servidor { get; set; } = "smtp.office365.com";
    public int Puerto { get; set; } = 587;
    public bool UsarSsl { get; set; } = true;
    public string Usuario { get; set; } = "";
    public string Clave { get; set; } = "";
    public string Remitente { get; set; } = "no-responder@goodoffice.local";
    public string NombreRemitente { get; set; } = "Good Office";
    public string CarpetaSalida { get; set; } = "correos";
}

/// <summary>
/// Envía las invitaciones de reunión al correo corporativo con un archivo .ics
/// adjunto, que Outlook o Gmail muestran como invitación de calendario.
/// </summary>
public class CorreoService(Microsoft.Extensions.Options.IOptions<CorreoOptions> opciones, ILogger<CorreoService> log)
{
    private readonly CorreoOptions _o = opciones.Value;

    public async Task<bool> EnviarInvitacionAsync(Reunion r, Usuario organizador, IEnumerable<string> invitados)
    {
        var destinatarios = invitados.Where(c => c.Contains('@')).ToList();
        if (destinatarios.Count == 0) return false;

        using var mail = new MailMessage
        {
            From = new MailAddress(_o.Remitente, _o.NombreRemitente),
            Subject = $"Invitación: {r.Titulo} ({r.Inicio:dd-MM-yyyy HH:mm})",
            Body = $"""
                Hola,

                {organizador.Nombre} te invita a la reunión "{r.Titulo}".

                Fecha: {r.Inicio:dddd dd 'de' MMMM, HH:mm} ({r.DuracionMinutos} min)
                Sala: {r.Sala}
                {r.Descripcion}

                Abre el archivo adjunto para agregarla a tu calendario.
                -- Good Office
                """,
            BodyEncoding = Encoding.UTF8,
            SubjectEncoding = Encoding.UTF8
        };
        mail.ReplyToList.Add(new MailAddress(organizador.Correo, organizador.Nombre));
        foreach (var d in destinatarios) mail.To.Add(d);

        var ics = CrearIcs(r, organizador, destinatarios);
        var adjunto = new Attachment(new MemoryStream(Encoding.UTF8.GetBytes(ics)), "invitacion.ics",
            "text/calendar; method=REQUEST; charset=UTF-8");
        mail.Attachments.Add(adjunto);

        using var smtp = new SmtpClient();
        if (_o.UsarSmtp)
        {
            smtp.Host = _o.Servidor;
            smtp.Port = _o.Puerto;
            smtp.EnableSsl = _o.UsarSsl;
            if (!string.IsNullOrEmpty(_o.Usuario))
                smtp.Credentials = new NetworkCredential(_o.Usuario, _o.Clave);
        }
        else
        {
            // Modo de prueba: el correo queda como .eml en la carpeta y se puede abrir con Outlook.
            var carpeta = Path.GetFullPath(_o.CarpetaSalida);
            Directory.CreateDirectory(carpeta);
            smtp.DeliveryMethod = SmtpDeliveryMethod.SpecifiedPickupDirectory;
            smtp.PickupDirectoryLocation = carpeta;
        }

        try
        {
            await smtp.SendMailAsync(mail);
            log.LogInformation("Invitación '{t}' enviada a {n} destinatarios ({modo})", r.Titulo, destinatarios.Count,
                _o.UsarSmtp ? "SMTP" : "archivo .eml");
            return true;
        }
        catch (Exception ex)
        {
            log.LogError(ex, "No se pudo enviar la invitación");
            return false;
        }
    }

    private static string CrearIcs(Reunion r, Usuario org, List<string> invitados)
    {
        static string F(DateTime d) => d.ToUniversalTime().ToString("yyyyMMdd'T'HHmmss'Z'");
        var sb = new StringBuilder();
        sb.AppendLine("BEGIN:VCALENDAR");
        sb.AppendLine("PRODID:-//Good Office//ES");
        sb.AppendLine("VERSION:2.0");
        sb.AppendLine("METHOD:REQUEST");
        sb.AppendLine("BEGIN:VEVENT");
        sb.AppendLine($"UID:reunion-{r.Id}@goodoffice.local");
        sb.AppendLine($"DTSTAMP:{F(DateTime.Now)}");
        sb.AppendLine($"DTSTART:{F(r.Inicio)}");
        sb.AppendLine($"DTEND:{F(r.Inicio.AddMinutes(r.DuracionMinutos))}");
        sb.AppendLine($"SUMMARY:{r.Titulo}");
        sb.AppendLine($"LOCATION:{r.Sala}");
        sb.AppendLine($"DESCRIPTION:{r.Descripcion.Replace("\n", "\\n")}");
        sb.AppendLine($"ORGANIZER;CN={org.Nombre}:mailto:{org.Correo}");
        foreach (var i in invitados)
            sb.AppendLine($"ATTENDEE;ROLE=REQ-PARTICIPANT;RSVP=TRUE:mailto:{i}");
        sb.AppendLine("END:VEVENT");
        sb.AppendLine("END:VCALENDAR");
        return sb.ToString();
    }
}
