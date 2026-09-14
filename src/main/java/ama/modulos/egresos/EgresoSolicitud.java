package ama.modulos.egresos;

import java.time.LocalDate;
import java.util.List;

public record EgresoSolicitud(LocalDate fecha, Long tipoId, String proveedor,
        String comprobante, String observacion, List<LineaEgreso> detalles) {}
