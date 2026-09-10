package ama.modulos.egresos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import static ama.modulos.egresos.EgresoException.datosInvalidos;

public final class EgresoValidacion {
    private static final BigDecimal MAX_IMPORTE = new BigDecimal("9999999999999999.99");
    private static final BigDecimal MAX_CANTIDAD = new BigDecimal("999999999.999");
    private static final int MAX_DETALLES = 100;

    private EgresoValidacion() {}

    public static String texto(String valor, int maximo, boolean requerido) {
        String limpio = valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
        if ((requerido && limpio.isEmpty()) || limpio.length() > maximo) {
            throw datosInvalidos("Texto obligatorio o demasiado largo (máximo " + maximo + ").");
        }
        return limpio;
    }

    public static void rango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            throw datosInvalidos("Rango de fechas no válido.");
        }
    }

    public static BigDecimal subtotal(LineaEgreso linea) {
        if (linea == null) throw datosInvalidos("Detalle no válido.");
        texto(linea.producto(), 180, true);
        positivo(linea.cantidad(), 3, MAX_CANTIDAD, "Cantidad");
        positivo(linea.precio(), 2, MAX_IMPORTE, "Precio");
        BigDecimal subtotal = linea.cantidad().multiply(linea.precio()).setScale(2, RoundingMode.HALF_UP);
        validarImporte(subtotal);
        return subtotal;
    }

    public static EgresoSolicitud normalizar(EgresoSolicitud solicitud) {
        if (solicitud == null || solicitud.fecha() == null || solicitud.tipoId() == null
                || solicitud.detalles() == null || solicitud.detalles().isEmpty()
                || solicitud.detalles().size() > MAX_DETALLES) {
            throw datosInvalidos("Complete fecha, tipo y entre 1 y 100 detalles.");
        }
        var lineas = solicitud.detalles().stream().map(linea -> {
            subtotal(linea);
            return new LineaEgreso(texto(linea.producto(), 180, true), linea.cantidad(), linea.precio());
        }).toList();
        return new EgresoSolicitud(solicitud.fecha(), solicitud.tipoId(), texto(solicitud.proveedor(), 180, true),
                texto(solicitud.comprobante(), 100, false), texto(solicitud.observacion(), 500, false), lineas);
    }

    public static BigDecimal total(EgresoSolicitud solicitud) {
        BigDecimal total = solicitud.detalles().stream().map(EgresoValidacion::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        validarImporte(total);
        return total;
    }

    private static void positivo(BigDecimal valor, int decimales, BigDecimal maximo, String campo) {
        if (valor == null || valor.signum() <= 0 || valor.scale() > decimales || valor.compareTo(maximo) > 0) {
            throw datosInvalidos(campo + " debe ser positivo, con hasta " + decimales + " decimales y dentro del límite permitido.");
        }
    }

    private static void validarImporte(BigDecimal valor) {
        if (valor.compareTo(MAX_IMPORTE) > 0) throw datosInvalidos("Importe demasiado grande.");
    }
}
