package ama.modulos.comprobantesv2;

public record AuditoriaComprobanteComparacion(
        Long codigo,
        String accion,
        String anterior,
        String nuevo) {
}
