package ama.modulos.egresos;

import java.math.BigDecimal;

public record LineaEgreso(String producto, BigDecimal cantidad, BigDecimal precio,
        Long productoId, Long categoriaId, String categoria) {
    public LineaEgreso(String producto, BigDecimal cantidad, BigDecimal precio) {
        this(producto, cantidad, precio, null, null, "");
    }

    public LineaEgreso(String producto, BigDecimal cantidad, BigDecimal precio, Long productoId) {
        this(producto, cantidad, precio, productoId, null, "");
    }
}
