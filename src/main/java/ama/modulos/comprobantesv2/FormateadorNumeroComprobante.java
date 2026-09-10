package ama.modulos.comprobantesv2;

import ama.dominio.Comprobante;

final class FormateadorNumeroComprobante {

    private static final int LONGITUD_ESTABLECIMIENTO = 3;
    private static final int LONGITUD_PUNTO_EXPEDICION = 3;
    private static final int LONGITUD_NUMERO = 7;

    private FormateadorNumeroComprobante() {
    }

    static String numeroFiscal(Comprobante comprobante) {
        var clave = comprobante.getComprobantePK();
        String establecimiento = primerValor(
                comprobante.getEstablecimientoFiscal(),
                comprobante.getPuntoExpedicion().getSucursal().getNombreSucursal(),
                clave.getPuntoExpedicionPK().getCodigoSucursal());
        String puntoExpedicion = primerValor(
                comprobante.getPuntoExpedicionFiscal(),
                comprobante.getPuntoExpedicion().getNombrePuntoExpedicion(),
                clave.getPuntoExpedicionPK().getCodigoPuntoExpedicion());

        return completarConCeros(establecimiento, LONGITUD_ESTABLECIMIENTO)
                + "-" + completarConCeros(puntoExpedicion, LONGITUD_PUNTO_EXPEDICION)
                + "-" + completarConCeros(String.valueOf(clave.getNumeroComprobante()), LONGITUD_NUMERO);
    }

    static String serieFiscal(Comprobante comprobante) {
        if (comprobante.getSerieFiscal() != null && !comprobante.getSerieFiscal().isBlank()) {
            return comprobante.getSerieFiscal().trim();
        }
        return comprobante.getSerie() == null ? "" : comprobante.getSerie().getSerie();
    }

    private static String primerValor(String valorFiscal, String valorConfigurado, Integer codigoInterno) {
        if (valorFiscal != null && !valorFiscal.isBlank()) {
            return valorFiscal.trim();
        }
        if (valorConfigurado != null && !valorConfigurado.isBlank()) {
            return valorConfigurado.trim();
        }
        return codigoInterno == null ? "" : codigoInterno.toString();
    }

    private static String completarConCeros(String valor, int longitud) {
        return String.format("%" + longitud + "s", valor).replace(' ', '0');
    }
}
