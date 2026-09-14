package ama.modulos.egresos;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogoRepository {
    private final JdbcTemplate jdbc;

    public CatalogoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CatalogoDto> buscar(int sucursal, ClaseCatalogo clase, String nombre) {
        return jdbc.query("""
                SELECT c.id, c.nombre, c.documento, c.telefono, c.direccion, c.correo, c.monto,
                       c.categoria_id, cat.nombre AS categoria
                  FROM egreso_catalogos c
                  LEFT JOIN egreso_catalogos cat
                    ON cat.id = c.categoria_id AND cat.codigo_sucursal = c.codigo_sucursal
                 WHERE c.codigo_sucursal = ? AND c.clase = ? AND LOCATE(?, c.nombre) > 0
                 ORDER BY c.nombre LIMIT 100
                """, (rs, fila) -> new CatalogoDto(rs.getLong("id"), rs.getString("nombre"),
                rs.getString("documento"), rs.getString("telefono"), rs.getString("direccion"),
                rs.getString("correo"), rs.getBigDecimal("monto"), rs.getObject("categoria_id", Long.class),
                rs.getString("categoria")),
                sucursal, clase.name(), nombre);
    }

    public boolean existe(int sucursal, ClaseCatalogo clase, long id) {
        Integer cantidad = jdbc.queryForObject("""
                SELECT COUNT(*) FROM egreso_catalogos
                 WHERE id = ? AND codigo_sucursal = ? AND clase = ?
                """, Integer.class, id, sucursal, clase.name());
        return Integer.valueOf(1).equals(cantidad);
    }

    public int actualizar(int sucursal, ClaseCatalogo clase, CatalogoDto dato) {
        return jdbc.update("""
                UPDATE egreso_catalogos SET nombre = ?, documento = ?, telefono = ?, direccion = ?, correo = ?, monto = ?, categoria_id = ?
                 WHERE id = ? AND codigo_sucursal = ? AND clase = ?
                """, dato.nombre(), dato.documento(), dato.telefono(), dato.direccion(), dato.correo(), dato.monto(),
                dato.categoriaId(),
                dato.id(), sucursal, clase.name());
    }

    public long crearOEncontrar(int sucursal, ClaseCatalogo clase, CatalogoDto dato) {
        // La clave única de MySQL también resuelve creaciones simultáneas del mismo nombre.
        jdbc.update("""
                INSERT INTO egreso_catalogos (codigo_sucursal, clase, nombre, documento, telefono, direccion, correo, monto, categoria_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE id = id
                """, sucursal, clase.name(), dato.nombre(), dato.documento(), dato.telefono(), dato.direccion(),
                dato.correo(), dato.monto(), dato.categoriaId());
        return jdbc.queryForObject("""
                SELECT id FROM egreso_catalogos WHERE codigo_sucursal = ? AND clase = ? AND nombre = ?
                """, Long.class, sucursal, clase.name(), dato.nombre());
    }
    public CatalogoDto encontrar(int sucursal, ClaseCatalogo clase, long id) {
        return jdbc.query("""
                SELECT c.id, c.nombre, c.documento, c.telefono, c.direccion, c.correo, c.monto,
                       c.categoria_id, cat.nombre AS categoria
                  FROM egreso_catalogos c
                  LEFT JOIN egreso_catalogos cat
                    ON cat.id = c.categoria_id AND cat.codigo_sucursal = c.codigo_sucursal
                 WHERE c.id = ? AND c.codigo_sucursal = ? AND c.clase = ?
                """, (rs, fila) -> new CatalogoDto(rs.getLong("id"), rs.getString("nombre"),
                rs.getString("documento"), rs.getString("telefono"), rs.getString("direccion"),
                rs.getString("correo"), rs.getBigDecimal("monto"), rs.getObject("categoria_id", Long.class),
                rs.getString("categoria")), id, sucursal, clase.name())
                .stream().findFirst().orElseThrow(() -> new EgresoException(
                        EgresoException.Motivo.NO_ENCONTRADO, "Registro no encontrado."));
    }

    public boolean categoriaProductoValida(int sucursal, Long id) {
        if (id == null) return true;
        Integer cantidad = jdbc.queryForObject("""
                SELECT COUNT(*) FROM egreso_catalogos
                 WHERE id = ? AND codigo_sucursal = ? AND clase = 'CATEGORIA_PRODUCTO'
                """, Integer.class, id, sucursal);
        return Integer.valueOf(1).equals(cantidad);
    }

    public void actualizarMonto(int sucursal, long producto, java.math.BigDecimal monto) {
        jdbc.update("UPDATE egreso_catalogos SET monto = ? WHERE id = ? AND codigo_sucursal = ? AND clase = 'PRODUCTO'",
                monto, producto, sucursal);
    }

    public int eliminar(int sucursal, ClaseCatalogo clase, long id) {
        return jdbc.update("DELETE FROM egreso_catalogos WHERE id = ? AND codigo_sucursal = ? AND clase = ?",
                id, sucursal, clase.name());
    }
}
