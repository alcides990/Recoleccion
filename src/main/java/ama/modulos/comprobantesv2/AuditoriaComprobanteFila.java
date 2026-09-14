package ama.modulos.comprobantesv2;

import java.time.LocalDateTime;

public record AuditoriaComprobanteFila(
        Long codigo,
        LocalDateTime fecha,
        String accion,
        String documento,
        String timbrado,
        String serie,
        String usuario,
        String motivo) {
}
