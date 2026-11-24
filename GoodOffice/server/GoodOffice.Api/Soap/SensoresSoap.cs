using System.Runtime.Serialization;
using System.ServiceModel;
using GoodOffice.Api.Services;

namespace GoodOffice.Api.Soap;

// Servicio web SOAP de ejemplo (XML + WSDL). Sirve para integrar Good Office
// con sistemas antiguos de la empresa (por ejemplo un BMS del edificio).
// WSDL: http://localhost:5080/soap/sensores.asmx?wsdl

[DataContract(Namespace = "http://goodoffice.local/sensores")]
public class LecturaSoap
{
    [DataMember(Order = 1)] public string Sala { get; set; } = "";
    [DataMember(Order = 2)] public string Tipo { get; set; } = "";
    [DataMember(Order = 3)] public double Valor { get; set; }
    [DataMember(Order = 4)] public string Unidad { get; set; } = "";
    [DataMember(Order = 5)] public string Estado { get; set; } = "";
    [DataMember(Order = 6)] public DateTime FechaHora { get; set; }
}

[ServiceContract(Namespace = "http://goodoffice.local/sensores")]
public interface ISensoresSoap
{
    [OperationContract]
    string[] ListarSalas();

    [OperationContract]
    LecturaSoap[] ObtenerAmbiente(string sala);

    [OperationContract]
    LecturaSoap RegistrarLectura(string sala, string tipo, double valor);
}

public class SensoresSoapService(SensorService sensores, Microsoft.Extensions.Options.IOptions<UmbralesOptions> umbrales) : ISensoresSoap
{
    public string[] ListarSalas() => sensores.SalasAsync().GetAwaiter().GetResult().ToArray();

    public LecturaSoap[] ObtenerAmbiente(string sala) =>
        sensores.AmbienteAsync(sala).GetAwaiter().GetResult().Sensores
            .Select(s => new LecturaSoap
            {
                Sala = sala, Tipo = s.Tipo, Valor = s.Valor, Unidad = s.Unidad, Estado = s.Estado, FechaHora = s.FechaHora
            }).ToArray();

    public LecturaSoap RegistrarLectura(string sala, string tipo, double valor)
    {
        var l = sensores.RegistrarAsync(sala, tipo, valor, "soap").GetAwaiter().GetResult();
        return new LecturaSoap
        {
            Sala = l.Sala, Tipo = l.Tipo, Valor = l.Valor, Unidad = l.Unidad,
            Estado = umbrales.Value.Clasificar(l.Tipo, l.Valor, l.FechaHora), FechaHora = l.FechaHora
        };
    }
}
