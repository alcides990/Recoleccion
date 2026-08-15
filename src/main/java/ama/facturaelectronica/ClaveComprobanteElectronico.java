package ama.facturaelectronica;

import ama.dominio.Comprobante;

public record ClaveComprobanteElectronico(Integer sucursal, Integer puntoExpedicion,
        Integer tipoComprobante, Integer serie, Integer numero) {

    public static ClaveComprobanteElectronico desde(Comprobante comprobante) {
        var pk = comprobante.getComprobantePK();
        return new ClaveComprobanteElectronico(
                pk.getPuntoExpedicionPK().getCodigoSucursal(),
                pk.getPuntoExpedicionPK().getCodigoPuntoExpedicion(),
                pk.getCodigoTipoComprobante(), pk.getCodigoSerie(), pk.getNumeroComprobante());
    }

    public String directorio() {
        return "%03d-%03d-%02d-%s-%07d".formatted(
                sucursal, puntoExpedicion, tipoComprobante,
                serie, numero);
    }
}
