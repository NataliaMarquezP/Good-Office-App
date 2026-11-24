using GoodOffice.Api.Data;
using GoodOffice.Api.Services;
using GoodOffice.Api.Soap;
using Microsoft.EntityFrameworkCore;
using Scalar.AspNetCore;
using SoapCore;

var builder = WebApplication.CreateBuilder(args);

// Secretos locales (por ejemplo la clave SMTP): appsettings.Local.json no se sube a Git.
builder.Configuration.AddJsonFile("appsettings.Local.json", optional: true, reloadOnChange: true);
builder.Configuration.AddEnvironmentVariables();

// Base de datos SQLite: un solo archivo (goodoffice.db), sin servidor ni permisos.
builder.Services.AddDbContext<GoodOfficeDb>(o =>
    o.UseSqlite(builder.Configuration.GetConnectionString("GoodOffice") ?? "Data Source=goodoffice.db"));

builder.Services.Configure<UmbralesOptions>(builder.Configuration.GetSection("Umbrales"));
builder.Services.Configure<CorreoOptions>(builder.Configuration.GetSection("Correo"));
builder.Services.AddScoped<SensorService>();
builder.Services.AddScoped<CorreoService>();
builder.Services.AddHostedService<SimuladorSensores>();

// REST: JSON por defecto, XML si el cliente pide "Accept: application/xml".
builder.Services.AddControllers(o => o.RespectBrowserAcceptHeader = true)
    .AddXmlSerializerFormatters()
    .AddJsonOptions(o => o.JsonSerializerOptions.Converters.Add(new FechaLocalConverter()));
builder.Services.AddOpenApi();

// SOAP (XML + WSDL)
builder.Services.AddSoapCore();
builder.Services.AddScoped<ISensoresSoap, SensoresSoapService>();

// La app Android y la documentación pueden llamar a la API desde cualquier origen.
builder.Services.AddCors(o => o.AddDefaultPolicy(p => p.AllowAnyOrigin().AllowAnyHeader().AllowAnyMethod()));

var app = builder.Build();

await DatosIniciales.CargarAsync(app.Services);

app.UseCors();
app.UseDefaultFiles();
app.UseStaticFiles();

((IApplicationBuilder)app).UseSoapEndpoint<ISensoresSoap>("/soap/sensores.asmx", new SoapEncoderOptions(), SoapSerializer.DataContractSerializer);

app.UseMiddleware<AutenticacionToken>();

// Documentación: /openapi/v1.json y una página interactiva en /docs
app.MapOpenApi();
app.MapScalarApiReference("/docs", o => o.WithTitle("Good Office API"));

app.MapControllers();

app.Run();

/// <summary>Escribe las fechas como "2026-11-23T10:30:00" (hora local, sin zona) para que la app las lea fácil.</summary>
class FechaLocalConverter : System.Text.Json.Serialization.JsonConverter<DateTime>
{
    public override DateTime Read(ref System.Text.Json.Utf8JsonReader r, Type t, System.Text.Json.JsonSerializerOptions o)
    {
        var d = r.GetDateTime();
        return d.Kind == DateTimeKind.Utc ? d.ToLocalTime() : DateTime.SpecifyKind(d, DateTimeKind.Unspecified);
    }

    public override void Write(System.Text.Json.Utf8JsonWriter w, DateTime v, System.Text.Json.JsonSerializerOptions o) =>
        w.WriteStringValue(v.ToString("yyyy-MM-dd'T'HH:mm:ss"));
}
