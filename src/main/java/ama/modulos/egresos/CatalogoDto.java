package ama.modulos.egresos;

import java.math.BigDecimal;

public record CatalogoDto(Long id, String nombre, String documento, String telefono,
        String direccion, String correo, BigDecimal monto, Long categoriaId, String categoria) {
    public CatalogoDto(Long id, String nombre, String documento, String telefono, String direccion) {
        this(id, nombre, documento, telefono, direccion, "", BigDecimal.ZERO, null, "");
    }

    public CatalogoDto(Long id, String nombre, String documento, String telefono,
            String direccion, String correo, BigDecimal monto) {
        this(id, nombre, documento, telefono, direccion, correo, monto, null, "");
    }

    public static CatalogoDto nuevo(String nombre) {
        return new CatalogoDto(null, nombre, "", "", "");
    }
}
