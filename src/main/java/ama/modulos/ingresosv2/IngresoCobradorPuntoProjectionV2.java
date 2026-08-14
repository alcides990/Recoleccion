package ama.modulos.ingresosv2;

public interface IngresoCobradorPuntoProjectionV2 {
    Integer getCodigoPuntoExpedicion();
    String getPuntoExpedicion();
    Integer getCodigoCobrador();
    String getCobrador();
    Double getImporte();
    Long getCantidad();
}
